package com.bag.audioandroid.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceFxProcessResult
import com.bag.audioandroid.domain.VoiceFxStreamProcessor
import com.bag.audioandroid.domain.VoiceLiveConfig
import com.bag.audioandroid.domain.VoiceLiveGateway
import com.bag.audioandroid.domain.VoiceLiveRouteSnapshot
import kotlin.concurrent.thread

class AndroidVoiceLiveGateway(
    context: Context,
    private val voiceFxGateway: VoiceFxGateway,
) : VoiceLiveGateway {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val deviceSelector = AndroidVoiceLiveDeviceSelector(audioManager)
    private val routingPolicy = AndroidVoiceLiveRoutingPolicy(audioManager)

    @Volatile
    private var running = false

    @Volatile
    private var workerThread: Thread? = null

    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @SuppressLint("MissingPermission")
    override fun start(
        config: VoiceLiveConfig,
        onRouteChanged: (VoiceLiveRouteSnapshot) -> Unit,
        onStopped: (errorCode: Int) -> Unit,
    ): Boolean {
        if (running || config.sampleRateHz <= 0) {
            return false
        }
        val processor =
            VoiceFxStreamProcessor.create(
                voiceFxGateway = voiceFxGateway,
                preset = config.preset,
                subvoiceStyle = config.subvoiceStyle,
                sampleRateHz = config.sampleRateHz,
            ) ?: return false

        val minRecordBytes =
            AudioRecord.getMinBufferSize(
                config.sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        val minTrackBytes =
            AudioTrack.getMinBufferSize(
                config.sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        if (minRecordBytes <= 0 || minTrackBytes <= 0) {
            processor.release()
            return false
        }

        val recordBufferBytes = maxOf(minRecordBytes, LIVE_BLOCK_SAMPLES * Short.SIZE_BYTES * 6)
        val trackBufferBytes = maxOf(minTrackBytes, LIVE_BLOCK_SAMPLES * Short.SIZE_BYTES * 8)
        val record =
            createAudioRecord(config.sampleRateHz, recordBufferBytes)
                ?: run {
                    processor.release()
                    return false
                }
        val track =
            createAudioTrack(config.sampleRateHz, trackBufferBytes)
                ?: run {
                    record.release()
                    processor.release()
                    return false
                }

        val preferredInputDevice = deviceSelector.choosePreferredInputDevice()
        val preferredOutputDevice = deviceSelector.choosePreferredOutputDevice()
        routingPolicy.apply(
            record = record,
            track = track,
            preferredInputDevice = preferredInputDevice,
            preferredOutputDevice = preferredOutputDevice,
        )

        val echoCanceler =
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
            } else {
                null
            }
        val noiseSuppressor =
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
            } else {
                null
            }
        val gainControl =
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(record.audioSessionId)?.apply { enabled = true }
            } else {
                null
            }

        audioRecord = record
        audioTrack = track
        running = true
        workerThread =
            thread(
                name = "VoiceLiveGateway",
                isDaemon = true,
            ) {
                val stopCode =
                    try {
                        runLiveLoop(
                            record = record,
                            track = track,
                            processor = processor,
                            onRouteChanged = onRouteChanged,
                            preferredOutputDevice = preferredOutputDevice,
                        )
                    } finally {
                        running = false
                        safelyStopRecord(record)
                        safelyStopTrack(track)
                        echoCanceler?.release()
                        noiseSuppressor?.release()
                        gainControl?.release()
                        record.release()
                        track.release()
                        processor.release()
                        routingPolicy.release()
                        audioRecord = null
                        audioTrack = null
                        workerThread = null
                    }
                onStopped(stopCode)
            }
        return true
    }

    override fun stop() {
        running = false
        audioRecord?.let(::safelyStopRecord)
        audioTrack?.pause()
        audioTrack?.flush()
        workerThread?.join(STOP_JOIN_TIMEOUT_MS)
        workerThread = null
        routingPolicy.release()
    }

    override fun release() {
        stop()
    }

    private fun runLiveLoop(
        record: AudioRecord,
        track: AudioTrack,
        processor: VoiceFxStreamProcessor,
        onRouteChanged: (VoiceLiveRouteSnapshot) -> Unit,
        preferredOutputDevice: AudioDeviceInfo?,
    ): Int {
        return try {
            record.startRecording()
            val buffer = ShortArray(LIVE_BLOCK_SAMPLES)
            val pendingOutput = ArrayList<ShortArray>(LIVE_START_BUFFER_BLOCKS)
            var playbackStarted = false
            while (running) {
                val readCount = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (readCount > 0) {
                    val dryBlock =
                        if (readCount == buffer.size) {
                            buffer
                        } else {
                            buffer.copyOf(readCount)
                        }
                    when (val result = processor.processBlock(dryBlock)) {
                        is VoiceFxProcessResult.Success -> {
                            if (!playbackStarted) {
                                pendingOutput += result.finalMix
                                if (pendingOutput.size < LIVE_START_BUFFER_BLOCKS) {
                                    continue
                                }
                                track.play()
                                onRouteChanged(
                                    VoiceLiveRouteSnapshot(
                                        inputRouteLabel =
                                            deviceSelector.routedDeviceLabel(
                                                record.routedDevice ?: deviceSelector.preferredDevice(record),
                                            ),
                                        outputRouteLabel =
                                            deviceSelector.routedDeviceLabel(
                                                track.routedDevice ?: deviceSelector.preferredDevice(track),
                                            ),
                                        speakerOutputRequested = preferredOutputDevice != null,
                                        speakerOutputActive =
                                            deviceSelector.isPreferredOutputDeviceActive(
                                                preferredOutputDevice = preferredOutputDevice,
                                                activeOutputDevice = track.routedDevice ?: deviceSelector.preferredDevice(track),
                                            ),
                                    ),
                                )
                                playbackStarted = true
                                for (pendingBlock in pendingOutput) {
                                    if (!writeAll(track, pendingBlock)) {
                                        return BagApiCodes.ERROR_INTERNAL
                                    }
                                }
                                pendingOutput.clear()
                                continue
                            }
                            if (!writeAll(track, result.finalMix)) {
                                return BagApiCodes.ERROR_INTERNAL
                            }
                        }
                        is VoiceFxProcessResult.Failed -> return result.errorCode
                    }
                } else if (
                    readCount == AudioRecord.ERROR_INVALID_OPERATION ||
                    readCount == AudioRecord.ERROR_BAD_VALUE
                ) {
                    return BagApiCodes.ERROR_INTERNAL
                }
            }
            if (!playbackStarted && pendingOutput.isNotEmpty()) {
                track.play()
                onRouteChanged(
                    VoiceLiveRouteSnapshot(
                        inputRouteLabel = deviceSelector.routedDeviceLabel(record.routedDevice ?: deviceSelector.preferredDevice(record)),
                        outputRouteLabel = deviceSelector.routedDeviceLabel(track.routedDevice ?: deviceSelector.preferredDevice(track)),
                        speakerOutputRequested = preferredOutputDevice != null,
                        speakerOutputActive =
                            deviceSelector.isPreferredOutputDeviceActive(
                                preferredOutputDevice = preferredOutputDevice,
                                activeOutputDevice = track.routedDevice ?: deviceSelector.preferredDevice(track),
                            ),
                    ),
                )
                playbackStarted = true
                for (pendingBlock in pendingOutput) {
                    if (!writeAll(track, pendingBlock)) {
                        return BagApiCodes.ERROR_INTERNAL
                    }
                }
            }
            when (val result = processor.flush()) {
                is VoiceFxProcessResult.Success -> {
                    if (!playbackStarted && result.finalMix.isNotEmpty()) {
                        track.play()
                        onRouteChanged(
                            VoiceLiveRouteSnapshot(
                                inputRouteLabel =
                                    deviceSelector.routedDeviceLabel(
                                        record.routedDevice ?: deviceSelector.preferredDevice(record),
                                    ),
                                outputRouteLabel =
                                    deviceSelector.routedDeviceLabel(
                                        track.routedDevice ?: deviceSelector.preferredDevice(track),
                                    ),
                                speakerOutputRequested = preferredOutputDevice != null,
                                speakerOutputActive =
                                    deviceSelector.isPreferredOutputDeviceActive(
                                        preferredOutputDevice = preferredOutputDevice,
                                        activeOutputDevice = track.routedDevice ?: deviceSelector.preferredDevice(track),
                                    ),
                            ),
                        )
                    }
                    if (result.finalMix.isNotEmpty() && !writeAll(track, result.finalMix)) {
                        BagApiCodes.ERROR_INTERNAL
                    } else {
                        BagApiCodes.ERROR_OK
                    }
                }
                is VoiceFxProcessResult.Failed -> result.errorCode
            }
        } catch (_: IllegalStateException) {
            BagApiCodes.ERROR_INTERNAL
        } catch (_: SecurityException) {
            BagApiCodes.ERROR_INTERNAL
        }
    }

    private fun writeAll(
        track: AudioTrack,
        pcm: ShortArray,
    ): Boolean {
        var offset = 0
        while (offset < pcm.size && running) {
            val written =
                track.write(
                    pcm,
                    offset,
                    pcm.size - offset,
                    AudioTrack.WRITE_BLOCKING,
                )
            if (written <= 0) {
                return false
            }
            offset += written
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(
        sampleRateHz: Int,
        bufferSizeBytes: Int,
    ): AudioRecord? {
        val sources =
            listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
            )
        sources.forEach { source ->
            val record =
                try {
                    AudioRecord(
                        source,
                        sampleRateHz,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeBytes,
                    )
                } catch (_: IllegalArgumentException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record?.release()
        }
        return null
    }

    private fun createAudioTrack(
        sampleRateHz: Int,
        bufferSizeBytes: Int,
    ): AudioTrack? =
        try {
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setSampleRate(sampleRateHz)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSizeBytes)
                .build()
                .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun safelyStopRecord(record: AudioRecord) {
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (_: IllegalStateException) {
        }
    }

    private fun safelyStopTrack(track: AudioTrack) {
        try {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop()
            }
        } catch (_: IllegalStateException) {
        }
    }

    private companion object {
        const val LIVE_BLOCK_SAMPLES = 2048
        const val LIVE_START_BUFFER_BLOCKS = 2
        const val STOP_JOIN_TIMEOUT_MS = 800L
    }
}
