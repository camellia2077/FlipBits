package com.bag.audioandroid.domain

data class DecodedAudioData(
    val wavStatusCode: Int,
    val metadataStatusCode: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val pcm: ShortArray,
    val metadata: GeneratedAudioMetadata? = null,
) {
    val isWavSuccess: Boolean
        get() = wavStatusCode == AudioIoWavCodes.STATUS_OK

    val hasReadableMetadata: Boolean
        get() = metadataStatusCode == AudioIoMetadataCodes.STATUS_OK && metadata != null
}
