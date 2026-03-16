package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway

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

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()
}
