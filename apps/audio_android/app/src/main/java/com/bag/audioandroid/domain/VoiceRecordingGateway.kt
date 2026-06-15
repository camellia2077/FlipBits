package com.bag.audioandroid.domain

interface VoiceRecordingGateway {
    fun startRecording(
        sampleRateHz: Int,
        onPcmChunk: ((ShortArray) -> Unit)? = null,
    ): Boolean

    fun stopRecording(): ShortArray

    fun release()
}
