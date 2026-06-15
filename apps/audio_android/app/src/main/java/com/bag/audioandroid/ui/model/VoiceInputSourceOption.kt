package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceInputSourceOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    Record(
        id = "record",
        labelResId = R.string.voice_input_source_record,
    ),
    Upload(
        id = "upload",
        labelResId = R.string.voice_input_source_upload,
    ),
    ;

    companion object {
        fun fromId(id: String?): VoiceInputSourceOption = entries.firstOrNull { it.id == id } ?: Record
    }
}
