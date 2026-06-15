package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoicePreviewTrackOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.model.defaultPreset

val DefaultVoiceTrackMode: VoiceTrackModeOption = VoiceTrackModeOption.Dual
val DefaultVoicePreset: VoiceFxPresetOption = DefaultVoiceTrackMode.defaultPreset()

data class VoiceSessionState(
    val hasRecordPermission: Boolean = false,
    val selectedWorkflowMode: VoiceWorkflowModeOption = VoiceWorkflowModeOption.Clip,
    val selectedTrackMode: VoiceTrackModeOption = DefaultVoiceTrackMode,
    val selectedInputSource: VoiceInputSourceOption = VoiceInputSourceOption.Record,
    val selectedRecordProcessingMode: VoiceRecordProcessingModeOption = VoiceRecordProcessingModeOption.AfterRecording,
    val isRecording: Boolean = false,
    val isLoadingInput: Boolean = false,
    val isProcessing: Boolean = false,
    val isLiveActive: Boolean = false,
    val inputPcm: ShortArray = shortArrayOf(),
    val inputDisplayName: String = "",
    val processedPcm: ShortArray = shortArrayOf(),
    val debugMainVoicePcm: ShortArray = shortArrayOf(),
    val debugSubvoicePcm: ShortArray = shortArrayOf(),
    val debugSignalOverlayPcm: ShortArray = shortArrayOf(),
    val sampleRateHz: Int = 44100,
    val selectedPreset: VoiceFxPresetOption = DefaultVoicePreset,
    val selectedSubvoiceStyle: VoiceFxSubvoiceStyleOption = VoiceFxSubvoiceStyleOption.Standard,
    val isPreviewPlaying: Boolean = false,
    val previewTrack: VoicePreviewTrackOption? = null,
    val previewPositionSamples: Int = 0,
    val liveInputRouteLabel: String = "",
    val liveOutputRouteLabel: String = "",
    val liveSpeakerOutRequested: Boolean = false,
    val liveSpeakerOutActive: Boolean = false,
    val lastErrorCode: Int = BagApiCodes.ERROR_OK,
) {
    val hasInputAudio: Boolean
        get() = inputPcm.isNotEmpty()

    val hasProcessedAudio: Boolean
        get() = processedPcm.isNotEmpty()

    val canStartRecording: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Clip &&
                selectedInputSource == VoiceInputSourceOption.Record &&
                hasRecordPermission &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canImportAudio: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Clip &&
                selectedInputSource == VoiceInputSourceOption.Upload &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canStopRecording: Boolean
        get() = isRecording

    val canProcess: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Clip &&
                hasInputAudio &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canPreview: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Clip &&
                hasProcessedAudio &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canExportResult: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Clip &&
                hasProcessedAudio &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canStartLive: Boolean
        get() =
            selectedWorkflowMode == VoiceWorkflowModeOption.Live &&
                selectedPreset.supportsLivePreview &&
                selectedPreset.trackMode == selectedTrackMode &&
                hasRecordPermission &&
                !isRecording &&
                !isLoadingInput &&
                !isProcessing &&
                !isLiveActive

    val canStopLive: Boolean
        get() = isLiveActive

    val inputDurationMs: Long
        get() = pcmDurationMillis(inputPcm.size, sampleRateHz)

    val processedDurationMs: Long
        get() = pcmDurationMillis(processedPcm.size, sampleRateHz)
}

val VoiceFxPresetOption.supportsLivePreview: Boolean
    get() =
        this == VoiceFxPresetOption.MachineVoice ||
            this == VoiceFxPresetOption.Binharic ||
            this == VoiceFxPresetOption.VoiceTrigger ||
            this == VoiceFxPresetOption.RawConstant ||
            this == VoiceFxPresetOption.SignalCant ||
            this == VoiceFxPresetOption.RobotVox

private fun pcmDurationMillis(
    sampleCount: Int,
    sampleRateHz: Int,
): Long {
    if (sampleCount <= 0 || sampleRateHz <= 0) {
        return 0L
    }
    return sampleCount.toLong() * 1000L / sampleRateHz.toLong()
}
