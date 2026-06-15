package com.bag.audioandroid.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.DecodedAudioData
import com.bag.audioandroid.domain.ImportedVoiceAudio
import com.bag.audioandroid.domain.VoiceAudioFileGateway
import com.bag.audioandroid.domain.VoiceAudioImportResult
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class AndroidVoiceAudioFileGateway(
    context: Context,
    private val audioIoGateway: AudioIoGateway,
) : VoiceAudioFileGateway {
    private val contentResolver = context.contentResolver

    override fun importVoiceAudio(uriString: String): VoiceAudioImportResult {
        val sourceUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return VoiceAudioImportResult.Failed
        decodeWithWavReader(sourceUri)?.let { return VoiceAudioImportResult.Success(it) }
        val decoded = decodeWithPlatformCodec(sourceUri) ?: return VoiceAudioImportResult.UnsupportedFormat
        return VoiceAudioImportResult.Success(decoded)
    }

    override fun exportVoiceAudioToDocument(
        pcm: ShortArray,
        sampleRateHz: Int,
        destinationUriString: String,
    ): Boolean {
        if (pcm.isEmpty() || sampleRateHz <= 0) {
            logExportFailure(
                "invalid voice export input pcmSamples=${pcm.size} sampleRateHz=$sampleRateHz " +
                    "destinationUri=$destinationUriString",
            )
            return false
        }
        val destinationUri =
            runCatching { Uri.parse(destinationUriString) }
                .getOrElse { throwable ->
                    logExportFailure("failed to parse destination uri=$destinationUriString", throwable)
                    return false
                }
        logExportDebug(
            "voice document export start uri=$destinationUri pcmSamples=${pcm.size} sampleRateHz=$sampleRateHz",
        )
        return try {
            val outputStream = contentResolver.openOutputStream(destinationUri)
            if (outputStream == null) {
                logExportFailure("openOutputStream returned null uri=$destinationUri")
                return false
            }
            outputStream.use {
                val wavBytes = audioIoGateway.encodeMonoPcm16ToWavBytes(sampleRateHz, pcm, metadata = null)
                if (wavBytes.isEmpty()) {
                    logExportFailure(
                        "WAV encode returned empty uri=$destinationUri pcmSamples=${pcm.size} " +
                            "sampleRateHz=$sampleRateHz",
                    )
                    return false
                }
                it.write(wavBytes)
                it.flush()
                logExportDebug("voice document export success uri=$destinationUri wavBytes=${wavBytes.size}")
            }
            true
        } catch (exception: IOException) {
            logExportFailure("voice document export IOException uri=$destinationUri", exception)
            false
        } catch (exception: SecurityException) {
            logExportFailure("voice document export SecurityException uri=$destinationUri", exception)
            false
        }
    }

    private fun resolveDisplayName(uri: Uri): String =
        contentResolver
            .query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)
                } else {
                    null
                }
            } ?: uri.lastPathSegment ?: "input_audio"

    private fun decodeWithWavReader(uri: Uri): ImportedVoiceAudio? {
        val sourceBytes =
            try {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } ?: return null
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(sourceBytes)
        if (!decoded.isDecodedMonoPcm16Wav()) {
            return null
        }
        return ImportedVoiceAudio(
            displayName = resolveDisplayName(uri),
            sampleRateHz = decoded.sampleRateHz,
            pcm = decoded.pcm,
        )
    }

    private fun decodeWithPlatformCodec(uri: Uri): ImportedVoiceAudio? {
        val descriptor =
            try {
                contentResolver.openFileDescriptor(uri, "r")
            } catch (_: SecurityException) {
                null
            } ?: return null
        descriptor.use { fileDescriptor ->
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(fileDescriptor.fileDescriptor)
                val trackIndex =
                    (0 until extractor.trackCount).firstOrNull { index ->
                        extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                    } ?: return null
                extractor.selectTrack(trackIndex)
                val inputFormat = extractor.getTrackFormat(trackIndex)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
                val codec =
                    try {
                        MediaCodec.createDecoderByType(mime)
                    } catch (_: IOException) {
                        return null
                    }
                try {
                    return decodeExtractorTrack(
                        extractor = extractor,
                        decoder = codec,
                        inputFormat = inputFormat,
                        displayName = resolveDisplayName(uri),
                    )
                } finally {
                    codec.release()
                }
            } catch (_: IOException) {
                return null
            } catch (_: IllegalArgumentException) {
                return null
            } finally {
                extractor.release()
            }
        }
    }

    private fun decodeExtractorTrack(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        inputFormat: MediaFormat,
        displayName: String,
    ): ImportedVoiceAudio? {
        val outputChunks = mutableListOf<ShortArray>()
        val bufferInfo = MediaCodec.BufferInfo()
        var outputSampleRateHz = inputFormat.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
        var outputChannelCount = inputFormat.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
        var outputEncoding = AudioFormat.ENCODING_PCM_16BIT
        var inputEnded = false
        var outputEnded = false

        try {
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()
            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer == null) {
                            return null
                        }
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.outputFormat
                        outputSampleRateHz = format.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputSampleRateHz
                        outputChannelCount = format.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannelCount
                        outputEncoding = format.getIntegerOrNull(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else ->
                        if (outputIndex >= 0) {
                            if (bufferInfo.size > 0) {
                                val outputBuffer = decoder.getOutputBuffer(outputIndex) ?: return null
                                val chunk =
                                    decodeOutputChunk(
                                        buffer = outputBuffer,
                                        bufferInfo = bufferInfo,
                                        channelCount = outputChannelCount,
                                        encoding = outputEncoding,
                                    ) ?: return null
                                if (chunk.isNotEmpty()) {
                                    outputChunks += chunk
                                }
                            }
                            outputEnded =
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            decoder.releaseOutputBuffer(outputIndex, false)
                        }
                }
            }
        } catch (_: IllegalStateException) {
            return null
        } finally {
            try {
                decoder.stop()
            } catch (_: IllegalStateException) {
            }
        }

        val totalSamples = outputChunks.sumOf { it.size }
        if (outputSampleRateHz <= 0 || totalSamples <= 0) {
            return null
        }
        val flattened = ShortArray(totalSamples)
        var offset = 0
        outputChunks.forEach { chunk ->
            chunk.copyInto(flattened, destinationOffset = offset)
            offset += chunk.size
        }
        return ImportedVoiceAudio(
            displayName = displayName,
            sampleRateHz = outputSampleRateHz,
            pcm = flattened,
        )
    }

    private fun decodeOutputChunk(
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        channelCount: Int,
        encoding: Int,
    ): ShortArray? {
        val channels = channelCount.coerceAtLeast(1)
        val duplicated =
            buffer.duplicate().apply {
                position(bufferInfo.offset)
                limit(bufferInfo.offset + bufferInfo.size)
                order(ByteOrder.LITTLE_ENDIAN)
            }
        return when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val frameCount = bufferInfo.size / (Float.SIZE_BYTES * channels)
                val output = ShortArray(frameCount)
                for (frameIndex in 0 until frameCount) {
                    var sum = 0f
                    repeat(channels) {
                        sum += duplicated.float
                    }
                    val mono = (sum / channels.toFloat()).coerceIn(-1f, 1f)
                    output[frameIndex] = (mono * Short.MAX_VALUE.toFloat()).roundToInt().toShort()
                }
                output
            }

            AudioFormat.ENCODING_PCM_16BIT -> {
                val frameCount = bufferInfo.size / (Short.SIZE_BYTES * channels)
                val output = ShortArray(frameCount)
                for (frameIndex in 0 until frameCount) {
                    var sum = 0
                    repeat(channels) {
                        sum += duplicated.short.toInt()
                    }
                    output[frameIndex] = (sum / channels).toShort()
                }
                output
            }

            else -> null
        }
    }

    private fun MediaFormat.getIntegerOrNull(key: String): Int? =
        if (containsKey(key)) {
            getInteger(key)
        } else {
            null
        }

    private fun DecodedAudioData.isDecodedMonoPcm16Wav(): Boolean =
        isWavSuccess &&
            wavStatusCode == AudioIoWavCodes.STATUS_OK &&
            channels == 1 &&
            sampleRateHz > 0 &&
            pcm.isNotEmpty()

    private companion object {
        const val VoiceExportTag = "VoiceExportDiag"
        const val CODEC_TIMEOUT_US = 10_000L

        fun logExportDebug(message: String) {
            try {
                Log.d(VoiceExportTag, message)
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
            }
        }

        fun logExportFailure(
            message: String,
            throwable: Throwable? = null,
        ) {
            try {
                if (throwable == null) {
                    Log.e(VoiceExportTag, message)
                } else {
                    Log.e(VoiceExportTag, message, throwable)
                }
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
            }
        }
    }
}
