package com.bag.audioandroid.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.bag.audioandroid.domain.VoiceFxRecordDiag
import com.bag.audioandroid.domain.VoiceRecordingGateway
import kotlin.concurrent.thread

class AndroidVoiceRecordingGateway : VoiceRecordingGateway {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var recordingThread: Thread? = null

    @Volatile
    private var recording = false

    private val chunkLock = Any()
    private val capturedChunks = mutableListOf<ShortArray>()
    private var capturedSampleCount = 0

    @SuppressLint("MissingPermission")
    override fun startRecording(
        sampleRateHz: Int,
        onPcmChunk: ((ShortArray) -> Unit)?,
    ): Boolean {
        if (recording || sampleRateHz <= 0) {
            return false
        }
        val minBufferBytes =
            AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        VoiceFxRecordDiag.log(
            "audioRecord minBufferBytes=$minBufferBytes requestedSr=$sampleRateHz",
        )
        if (minBufferBytes <= 0) {
            return false
        }
        val configuredBufferBytes = maxOf(minBufferBytes, sampleRateHz / 2)
        val record =
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    configuredBufferBytes,
                )
            } catch (_: IllegalArgumentException) {
                return false
            } catch (_: SecurityException) {
                return false
            }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        synchronized(chunkLock) {
            capturedChunks.clear()
            capturedSampleCount = 0
        }
        audioRecord = record
        recording = true
        try {
            record.startRecording()
        } catch (_: IllegalStateException) {
            recording = false
            record.release()
            audioRecord = null
            return false
        } catch (_: SecurityException) {
            recording = false
            record.release()
            audioRecord = null
            return false
        }

        recordingThread =
            thread(
                name = "VoiceRecordingGateway",
                isDaemon = true,
            ) {
                val buffer = ShortArray(2048)
                var readIndex = 0
                var readTotal = 0
                VoiceFxRecordDiag.log(
                    "audioRecord thread-start sr=$sampleRateHz configuredBufferBytes=$configuredBufferBytes " +
                        "readBufferSamples=${buffer.size}",
                )
                while (recording) {
                    val readCount = record.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        readIndex += 1
                        readTotal += readCount
                        val chunk = buffer.copyOf(readCount)
                        synchronized(chunkLock) {
                            capturedChunks.add(chunk)
                            capturedSampleCount += readCount
                        }
                        if (VoiceFxRecordDiag.shouldLogBlock(readIndex)) {
                            VoiceFxRecordDiag.log(
                                "audioRecord read index=$readIndex samples=$readCount total=$readTotal",
                            )
                        }
                        onPcmChunk?.invoke(chunk)
                    } else if (
                        readCount == AudioRecord.ERROR_INVALID_OPERATION ||
                        readCount == AudioRecord.ERROR_BAD_VALUE
                    ) {
                        VoiceFxRecordDiag.log(
                            "audioRecord read-error code=$readCount index=$readIndex total=$readTotal",
                        )
                        break
                    }
                }
                VoiceFxRecordDiag.log(
                    "audioRecord thread-stop reads=$readIndex totalSamples=$readTotal recording=$recording",
                )
            }
        return true
    }

    override fun stopRecording(): ShortArray {
        val record = audioRecord
        recording = false
        if (record != null) {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (_: IllegalStateException) {
            }
        }
        recordingThread?.join(500L)
        recordingThread = null
        audioRecord = null
        record?.release()

        return synchronized(chunkLock) {
            VoiceFxRecordDiag.log(
                "audioRecord stop capturedSamples=$capturedSampleCount chunks=${capturedChunks.size}",
            )
            if (capturedSampleCount <= 0) {
                capturedChunks.clear()
                capturedSampleCount = 0
                return@synchronized shortArrayOf()
            }
            val flattened = ShortArray(capturedSampleCount)
            var offset = 0
            capturedChunks.forEach { chunk ->
                chunk.copyInto(flattened, destinationOffset = offset)
                offset += chunk.size
            }
            capturedChunks.clear()
            capturedSampleCount = 0
            flattened
        }
    }

    override fun release() {
        if (recording || audioRecord != null) {
            stopRecording()
        }
        synchronized(chunkLock) {
            capturedChunks.clear()
            capturedSampleCount = 0
        }
    }
}
