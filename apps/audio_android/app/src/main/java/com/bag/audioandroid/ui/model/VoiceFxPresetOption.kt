package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class VoiceFxPresetOption(
    val id: String,
    val nativeValue: Int,
    val trackMode: VoiceTrackModeOption,
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int,
) {
    MachineVoice(
        id = "machine_voice",
        nativeValue = 0,
        trackMode = VoiceTrackModeOption.Single,
        labelResId = R.string.voice_metal_title,
        descriptionResId = R.string.voice_metal_summary,
    ),
    Binharic(
        id = "binharic",
        nativeValue = 1,
        trackMode = VoiceTrackModeOption.Dual,
        labelResId = R.string.voice_binharic_title,
        descriptionResId = R.string.voice_binharic_summary,
    ),
    VoiceTrigger(
        id = "voice_trigger",
        nativeValue = 5,
        trackMode = VoiceTrackModeOption.Dual,
        labelResId = R.string.voice_voice_trigger_title,
        descriptionResId = R.string.voice_voice_trigger_summary,
    ),
    RawConstant(
        id = "raw_constant",
        nativeValue = 4,
        trackMode = VoiceTrackModeOption.Dual,
        labelResId = R.string.voice_raw_title,
        descriptionResId = R.string.voice_raw_summary,
    ),
    SignalCant(
        id = "signal_cant",
        nativeValue = 2,
        trackMode = VoiceTrackModeOption.Single,
        labelResId = R.string.voice_code_title,
        descriptionResId = R.string.voice_code_summary,
    ),
    RobotVox(
        id = "robot_vox",
        nativeValue = 3,
        trackMode = VoiceTrackModeOption.Single,
        labelResId = R.string.voice_robot_title,
        descriptionResId = R.string.voice_robot_summary,
    ),
    ;

    companion object {
        fun fromId(id: String?): VoiceFxPresetOption = entries.firstOrNull { it.id == id } ?: MachineVoice
    }
}
