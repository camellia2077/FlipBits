package com.bag.audioandroid.domain

interface AudioIoGateway {
    fun encodeMonoPcm16ToWavBytes(sampleRateHz: Int, pcm: ShortArray): ByteArray
    fun decodeMonoPcm16WavBytes(wavBytes: ByteArray): DecodedAudioData
}
