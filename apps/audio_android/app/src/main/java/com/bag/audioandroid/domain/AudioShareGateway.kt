package com.bag.audioandroid.domain

interface AudioShareGateway {
    fun shareSavedAudio(item: SavedAudioItem): Boolean

    fun shareGeneratedAudio(
        displayName: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): Boolean = false

    fun shareAudio(
        displayName: String,
        uriString: String,
    ): Boolean
}
