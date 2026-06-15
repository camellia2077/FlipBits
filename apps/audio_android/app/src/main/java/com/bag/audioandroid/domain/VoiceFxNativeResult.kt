package com.bag.audioandroid.domain

import androidx.annotation.Keep

@Keep
data class VoiceFxNativeResult(
    val finalMix: ShortArray,
    val mainVoice: ShortArray,
    val subvoice: ShortArray,
    val signalOverlay: ShortArray,
    val errorCode: Int,
)
