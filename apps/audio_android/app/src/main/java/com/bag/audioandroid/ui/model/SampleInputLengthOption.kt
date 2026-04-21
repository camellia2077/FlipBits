package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class SampleInputLengthOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    Short("short", R.string.audio_sample_length_short),
    Long("long", R.string.audio_sample_length_long),
}
