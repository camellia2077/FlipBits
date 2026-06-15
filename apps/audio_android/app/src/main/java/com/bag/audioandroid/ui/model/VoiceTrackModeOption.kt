package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceTrackModeOption(
    val id: String,
    @param:StringRes val labelResId: Int,
) {
    Single(
        id = "single",
        labelResId = R.string.voice_track_mode_single,
    ),
    Dual(
        id = "dual",
        labelResId = R.string.voice_track_mode_dual,
    ),
    ;

    companion object {
        fun fromIdOrNull(id: String?): VoiceTrackModeOption? = entries.firstOrNull { it.id == id }

        fun fromId(id: String?): VoiceTrackModeOption = fromIdOrNull(id) ?: Single
    }
}

fun VoiceTrackModeOption.availablePresets(): List<VoiceFxPresetOption> = VoiceFxPresetOption.entries.filter { it.trackMode == this }

fun VoiceTrackModeOption.defaultPreset(): VoiceFxPresetOption = availablePresets().first()
