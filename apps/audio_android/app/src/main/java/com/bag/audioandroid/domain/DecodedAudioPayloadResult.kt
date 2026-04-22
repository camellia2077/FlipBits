package com.bag.audioandroid.domain

data class DecodedAudioPayloadResult(
    val decodedPayload: DecodedPayloadViewData = DecodedPayloadViewData.Empty,
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
)
