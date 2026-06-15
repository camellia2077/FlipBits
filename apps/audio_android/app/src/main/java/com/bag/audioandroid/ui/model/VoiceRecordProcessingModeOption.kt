package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceRecordProcessingModeOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    AfterRecording(
        id = "after_recording",
        labelResId = R.string.voice_record_processing_after_recording,
    ),
    WhileRecording(
        id = "while_recording",
        labelResId = R.string.voice_record_processing_while_recording,
    ),
}
