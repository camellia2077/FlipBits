package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioVisualizationFrame
import com.bag.audioandroid.domain.AudioVisualizationRegion
import com.bag.audioandroid.domain.AudioVisualizationTrack
import kotlin.math.roundToInt

class NativeAudioCodecGateway : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): Int = NativeBagBridge.nativeValidateEncodeRequest(
        text,
        sampleRateHz,
        frameSamples,
        mode,
        flashSignalProfile,
        flashVoicingFlavor
    )

    override fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): ShortArray = NativeBagBridge.nativeEncodeTextToPcm(
        text,
        sampleRateHz,
        frameSamples,
        mode,
        flashSignalProfile,
        flashVoicingFlavor
    )

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): Int = NativeBagBridge.nativeValidateDecodeConfig(
        sampleRateHz,
        frameSamples,
        mode,
        flashSignalProfile,
        flashVoicingFlavor
    )

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): String = NativeBagBridge.nativeDecodeGeneratedPcm(
        pcm,
        sampleRateHz,
        frameSamples,
        mode,
        flashSignalProfile,
        flashVoicingFlavor
    )

    override fun analyzeVisualization(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int
    ): AudioVisualizationTrack? {
        val packed = NativeBagBridge.nativeAnalyzeVisualization(
            pcm,
            sampleRateHz,
            frameSamples,
            mode,
            flashSignalProfile,
            flashVoicingFlavor
        )
        return decodeVisualizationTrack(packed)
    }

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()

    private fun decodeVisualizationTrack(raw: FloatArray): AudioVisualizationTrack? {
        if (raw.size < VISUALIZATION_HEADER_COUNT) {
            return null
        }
        val frameCount = raw[0].roundToInt()
        val totalSamples = raw[1].roundToInt()
        val sampleRateHz = raw[2].roundToInt()
        val frameStrideSamples = raw[3].roundToInt()
        if (frameCount < 0) {
            return null
        }
        val expectedCount = VISUALIZATION_HEADER_COUNT + frameCount * VISUALIZATION_FRAME_FIELD_COUNT
        if (raw.size < expectedCount) {
            return null
        }

        val frames = buildList(frameCount) {
            for (index in 0 until frameCount) {
                val base = VISUALIZATION_HEADER_COUNT + index * VISUALIZATION_FRAME_FIELD_COUNT
                add(
                    AudioVisualizationFrame(
                        sampleOffset = raw[base + 0].roundToInt(),
                        sampleCount = raw[base + 1].roundToInt(),
                        rms = raw[base + 2],
                        peak = raw[base + 3],
                        brightness = raw[base + 4],
                        region = AudioVisualizationRegion.fromNativeValue(raw[base + 5].roundToInt())
                    )
                )
            }
        }
        return AudioVisualizationTrack(
            frames = frames,
            totalSamples = totalSamples,
            sampleRateHz = sampleRateHz,
            frameStrideSamples = frameStrideSamples
        )
    }

    private companion object {
        const val VISUALIZATION_HEADER_COUNT = 4
        const val VISUALIZATION_FRAME_FIELD_COUNT = 6
    }
}
