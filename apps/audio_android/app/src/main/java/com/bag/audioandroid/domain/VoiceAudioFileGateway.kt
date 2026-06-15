package com.bag.audioandroid.domain

data class ImportedVoiceAudio(
    val displayName: String,
    val sampleRateHz: Int,
    val pcm: ShortArray,
)

sealed interface VoiceAudioImportResult {
    data class Success(
        val audio: ImportedVoiceAudio,
    ) : VoiceAudioImportResult

    data object UnsupportedFormat : VoiceAudioImportResult

    data object Failed : VoiceAudioImportResult
}

interface VoiceAudioFileGateway {
    fun importVoiceAudio(uriString: String): VoiceAudioImportResult

    fun exportVoiceAudioToDocument(
        pcm: ShortArray,
        sampleRateHz: Int,
        destinationUriString: String,
    ): Boolean
}
