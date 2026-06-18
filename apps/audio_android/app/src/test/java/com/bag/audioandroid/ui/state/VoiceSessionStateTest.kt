package com.bag.audioandroid.ui.state

import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSessionStateTest {
    @Test
    fun computedFlagsTrackVoiceFlowStates() {
        val initial = VoiceSessionState()
        assertFalse(initial.canStartRecording)
        assertFalse(initial.canImportAudio)
        assertFalse(initial.canProcess)
        assertFalse(initial.canPreview)
        assertFalse(initial.canStartLive)
        assertEquals(VoiceRecordProcessingModeOption.AfterRecording, initial.selectedRecordProcessingMode)
        assertEquals(VoiceTrackModeOption.Dual, initial.selectedTrackMode)
        assertEquals(VoiceFxPresetOption.BinaricCant, initial.selectedPreset)

        val readyToRecord = initial.copy(hasRecordPermission = true)
        assertTrue(readyToRecord.canStartRecording)

        val readyToGoLive =
            initial.copy(
                hasRecordPermission = true,
                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
            )
        assertTrue(readyToGoLive.canStartLive)

        val binaricCantLive =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Dual,
                selectedPreset = VoiceFxPresetOption.BinaricCant,
            )
        assertTrue(binaricCantLive.canStartLive)

        val rawDualTrackLive =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Dual,
                selectedPreset = VoiceFxPresetOption.RawConstant,
            )
        assertTrue(rawDualTrackLive.canStartLive)

        val voiceTriggerDualTrackLive =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Dual,
                selectedPreset = VoiceFxPresetOption.VoiceTrigger,
            )
        assertTrue(voiceTriggerDualTrackLive.canStartLive)

        val binaricCantLiveWrongTrack =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Single,
                selectedPreset = VoiceFxPresetOption.BinaricCant,
            )
        assertFalse(binaricCantLiveWrongTrack.canStartLive)

        val signalCantLive =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Single,
                selectedPreset = VoiceFxPresetOption.SignalCant,
            )
        assertTrue(signalCantLive.canStartLive)

        val robotVoxLive =
            readyToGoLive.copy(
                selectedTrackMode = VoiceTrackModeOption.Single,
                selectedPreset = VoiceFxPresetOption.RobotVox,
            )
        assertTrue(robotVoxLive.canStartLive)

        val recorded = readyToRecord.copy(inputPcm = ShortArray(44100) { 1 })
        assertTrue(recorded.canProcess)
        assertFalse(recorded.canPreview)

        val processed = recorded.copy(processedPcm = ShortArray(44100) { 2 })
        assertTrue(processed.canPreview)
        assertTrue(processed.canExportResult)

        val recording = processed.copy(isRecording = true)
        assertTrue(recording.canStopRecording)
        assertFalse(recording.canPreview)

        val liveRunning = readyToGoLive.copy(isLiveActive = true)
        assertTrue(liveRunning.canStopLive)
        assertFalse(liveRunning.canStartLive)
    }
}
