package com.bag.audioandroid.domain

data class DecodedAudioData(
    val statusCode: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val pcm: ShortArray
) {
    val isSuccess: Boolean
        get() = statusCode == AudioIoCodes.STATUS_OK
}
