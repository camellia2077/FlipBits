package com.bag.audioandroid

object NativeBagBridge {
    init {
        System.loadLibrary("audio_android_jni")
    }

    external fun nativeEncodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int
    ): ShortArray

    external fun nativeDecodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int
    ): String

    external fun nativeGetCoreVersion(): String
}
