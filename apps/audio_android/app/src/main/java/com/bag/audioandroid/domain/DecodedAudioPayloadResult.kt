package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class DecodedAudioPayloadResult(
    val decodedPayload: DecodedPayloadViewData = DecodedPayloadViewData.Empty,
    val followData: PayloadFollowViewData = PayloadFollowViewData.Empty,
)
