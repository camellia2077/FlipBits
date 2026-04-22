package com.bag.audioandroid.domain

data class EncodedAudioPayloadResult(
    val pcm: ShortArray = shortArrayOf(),
    val rawBytesHex: String = "",
    val rawBitsBinary: String = "",
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
)
