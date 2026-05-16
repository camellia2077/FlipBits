package com.bag.audioandroid.domain

sealed interface AudioExportResult {
    data class Success(
        val displayName: String,
        val uriString: String,
    ) : AudioExportResult

    data object Failed : AudioExportResult
}

interface AudioExportGateway {
    fun suggestGeneratedAudioDisplayName(
        inputText: String,
        metadata: GeneratedAudioMetadata,
    ): String

    fun exportGeneratedAudio(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult

    fun exportGeneratedAudioToDocument(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean
}
