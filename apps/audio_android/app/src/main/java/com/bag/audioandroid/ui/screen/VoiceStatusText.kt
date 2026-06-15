package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.state.VoiceSessionState

@Composable
internal fun voiceProcessHintText(session: VoiceSessionState): String? =
    when {
        session.isProcessing -> stringResource(R.string.voice_process_hint_processing)
        !session.hasInputAudio -> stringResource(R.string.voice_process_hint_needs_input)
        else -> null
    }

@Composable
internal fun inputReadyText(session: VoiceSessionState): String =
    if (session.selectedInputSource == VoiceInputSourceOption.Record) {
        formatDurationMillis(session.inputDurationMs)
    } else {
        val displayName = session.inputDisplayName.ifBlank { stringResource(R.string.voice_input_source_upload) }
        "$displayName • ${formatDurationMillis(session.inputDurationMs)}"
    }

@Composable
internal fun voiceStatusText(session: VoiceSessionState): String {
    if (session.selectedWorkflowMode == VoiceWorkflowModeOption.Live) {
        when {
            session.isLiveActive -> return stringResource(R.string.voice_status_live_running)
            !session.hasRecordPermission -> return stringResource(R.string.voice_status_permission_needed)
            session.lastErrorCode != BagApiCodes.ERROR_OK -> return stringResource(R.string.voice_status_live_failed)
            else -> return stringResource(R.string.voice_status_live_ready)
        }
    }
    return when {
        session.isRecording -> stringResource(R.string.voice_status_recording)
        session.isLoadingInput -> stringResource(R.string.voice_status_importing)
        session.isProcessing -> stringResource(R.string.voice_action_process_busy)
        session.selectedInputSource == VoiceInputSourceOption.Record && !session.hasRecordPermission ->
            stringResource(R.string.voice_status_permission_needed)
        session.selectedInputSource == VoiceInputSourceOption.Upload &&
            session.lastErrorCode == AudioIoWavCodes.STATUS_UNSUPPORTED_FORMAT ->
            stringResource(R.string.voice_status_import_unsupported)
        session.selectedInputSource == VoiceInputSourceOption.Upload &&
            session.lastErrorCode != BagApiCodes.ERROR_OK ->
            stringResource(R.string.voice_status_import_failed)
        session.lastErrorCode != BagApiCodes.ERROR_OK -> stringResource(R.string.voice_status_processing_failed)
        session.selectedInputSource == VoiceInputSourceOption.Upload -> stringResource(R.string.voice_status_upload_ready)
        else -> stringResource(R.string.voice_status_ready)
    }
}

@Composable
internal fun liveRouteText(session: VoiceSessionState): String {
    val input = session.liveInputRouteLabel.ifBlank { stringResource(R.string.voice_live_route_unknown) }
    val output = session.liveOutputRouteLabel.ifBlank { stringResource(R.string.voice_live_route_unknown) }
    return if (session.liveSpeakerOutRequested && !session.liveSpeakerOutActive) {
        stringResource(R.string.voice_live_route_requested_fallback, input, output)
    } else {
        stringResource(R.string.voice_live_route_active, input, output)
    }
}
