package com.bag.audioandroid.data

import com.bag.audioandroid.NativeBagBridge
import com.bag.audioandroid.domain.AudioCodecGateway

class NativeAudioCodecGateway : AudioCodecGateway {
    override fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int
    ): ShortArray = NativeBagBridge.nativeEncodeTextToPcm(text, sampleRateHz, frameSamples)

    override fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int
    ): String = NativeBagBridge.nativeDecodeGeneratedPcm(pcm, sampleRateHz, frameSamples)

    override fun getCoreVersion(): String = NativeBagBridge.nativeGetCoreVersion()
}
