package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.state.VoiceSessionState

@Composable
internal fun RecordInputActions(
    session: VoiceSessionState,
    onRequestRecordPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val startRecordingDescription = stringResource(R.string.voice_action_start_recording)
    val stopRecordingDescription = stringResource(R.string.voice_action_stop_recording)
    if (!session.hasRecordPermission) {
        Text(
            text = stringResource(R.string.voice_record_permission_required),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onRequestRecordPermission,
            modifier = Modifier.fillMaxWidth().testTag("voice-grant-record-permission-button"),
        ) {
            Text(text = stringResource(R.string.voice_action_grant_permission))
        }
    } else if (session.isRecording) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            FilledIconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopRecording()
                },
                modifier =
                    Modifier
                        .size(56.dp)
                        .testTag("voice-stop-recording-button")
                        .semantics { contentDescription = stopRecordingDescription },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = null,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            FilledIconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartRecording()
                },
                enabled = session.canStartRecording,
                modifier =
                    Modifier
                        .size(56.dp)
                        .testTag("voice-start-recording-button")
                        .semantics { contentDescription = startRecordingDescription },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
internal fun LiveInputActions(
    session: VoiceSessionState,
    onRequestRecordPermission: () -> Unit,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
) {
    if (!session.hasRecordPermission) {
        Text(
            text = stringResource(R.string.voice_record_permission_required),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onRequestRecordPermission,
            modifier = Modifier.fillMaxWidth().testTag("voice-grant-live-permission-button"),
        ) {
            Text(text = stringResource(R.string.voice_action_grant_permission))
        }
    } else if (session.isLiveActive) {
        Button(
            onClick = onStopLive,
            modifier = Modifier.fillMaxWidth().testTag("voice-stop-live-button"),
        ) {
            Text(text = stringResource(R.string.voice_action_stop_live))
        }
    } else {
        Button(
            onClick = onStartLive,
            enabled = session.canStartLive,
            modifier = Modifier.fillMaxWidth().testTag("voice-start-live-button"),
        ) {
            Text(text = stringResource(R.string.voice_action_start_live))
        }
    }
}
