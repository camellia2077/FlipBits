package com.bag.audioandroid.domain

interface AudioCodecGateway {
    fun validateEncodeRequest(text: String, sampleRateHz: Int, frameSamples: Int, mode: Int): Int
    fun encodeTextToPcm(text: String, sampleRateHz: Int, frameSamples: Int, mode: Int): ShortArray
    fun validateDecodeConfig(sampleRateHz: Int, frameSamples: Int, mode: Int): Int
    fun decodeGeneratedPcm(pcm: ShortArray, sampleRateHz: Int, frameSamples: Int, mode: Int): String
    fun getCoreVersion(): String
}
