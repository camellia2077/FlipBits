package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceWorkflowModeOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    Clip(
        id = "clip",
        labelResId = R.string.voice_workflow_clip,
    ),
    Live(
        id = "live",
        labelResId = R.string.voice_workflow_live,
    ),
    ;

    companion object {
        fun fromId(id: String?): VoiceWorkflowModeOption = entries.firstOrNull { it.id == id } ?: Clip
    }
}
