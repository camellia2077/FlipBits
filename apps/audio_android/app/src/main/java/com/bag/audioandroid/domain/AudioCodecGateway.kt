package com.bag.audioandroid.domain

interface AudioCodecGateway {
    fun encodeTextToPcm(text: String, sampleRateHz: Int, frameSamples: Int): ShortArray
    fun decodeGeneratedPcm(pcm: ShortArray, sampleRateHz: Int, frameSamples: Int): String
    fun getCoreVersion(): String
}
