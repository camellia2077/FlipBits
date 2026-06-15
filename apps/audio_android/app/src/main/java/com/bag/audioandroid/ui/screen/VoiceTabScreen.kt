package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoicePreviewTrackOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.state.VoiceSessionState

@Composable
internal fun VoiceTabScreen(
    selectedThemeStyle: ThemeStyleOption,
    session: VoiceSessionState,
    onRequestRecordPermission: () -> Unit,
    onWorkflowModeSelected: (VoiceWorkflowModeOption) -> Unit,
    onTrackModeSelected: (VoiceTrackModeOption) -> Unit,
    onInputSourceSelected: (VoiceInputSourceOption) -> Unit,
    onRecordProcessingModeSelected: (VoiceRecordProcessingModeOption) -> Unit,
    onPresetSelected: (VoiceFxPresetOption) -> Unit,
    onSubvoiceStyleSelected: (VoiceFxSubvoiceStyleOption) -> Unit,
    onImportAudio: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    onProcess: () -> Unit,
    onTogglePreview: () -> Unit,
    onTogglePreviewTrack: (VoicePreviewTrackOption) -> Unit = { onTogglePreview() },
    onPreviewTrackSeek: (VoicePreviewTrackOption, Int) -> Unit = { _, _ -> },
    onExportResult: () -> Unit,
    onShareOutput: () -> Unit = {},
    onClear: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    var hasShownLivePreviewDialog by rememberSaveable { mutableStateOf(false) }
    var isLivePreviewDialogOpen by rememberSaveable { mutableStateOf(false) }
    val selectWorkflowMode: (VoiceWorkflowModeOption) -> Unit = { mode ->
        if (
            mode == VoiceWorkflowModeOption.Live &&
            session.selectedWorkflowMode != VoiceWorkflowModeOption.Live &&
            !hasShownLivePreviewDialog
        ) {
            isLivePreviewDialogOpen = true
        } else {
            onWorkflowModeSelected(mode)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
    ) {
        VoiceWorkflowSwitcherBar(
            selectedWorkflowMode = session.selectedWorkflowMode,
            onWorkflowModeSelected = selectWorkflowMode,
            enabled =
                !session.isRecording &&
                    !session.isLoadingInput &&
                    !session.isProcessing &&
                    !session.isLiveActive,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = VoiceWorkflowSwitcherBarReservedHeight)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (session.selectedWorkflowMode == VoiceWorkflowModeOption.Clip) {
                VoiceClipWorkflowCard(
                    selectedThemeStyle = selectedThemeStyle,
                    session = session,
                    onRequestRecordPermission = onRequestRecordPermission,
                    onTrackModeSelected = onTrackModeSelected,
                    onInputSourceSelected = onInputSourceSelected,
                    onRecordProcessingModeSelected = onRecordProcessingModeSelected,
                    onPresetSelected = onPresetSelected,
                    onSubvoiceStyleSelected = onSubvoiceStyleSelected,
                    onImportAudio = onImportAudio,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onProcess = onProcess,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VoiceLiveWorkflowCard(
                    selectedThemeStyle = selectedThemeStyle,
                    session = session,
                    onRequestRecordPermission = onRequestRecordPermission,
                    onTrackModeSelected = onTrackModeSelected,
                    onPresetSelected = onPresetSelected,
                    onSubvoiceStyleSelected = onSubvoiceStyleSelected,
                    onStartLive = onStartLive,
                    onStopLive = onStopLive,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (session.selectedWorkflowMode == VoiceWorkflowModeOption.Clip) {
                VoiceSectionContainer(
                    selectedThemeStyle = selectedThemeStyle,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("voice-result-card"),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.voice_result_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text =
                                if (session.hasProcessedAudio) {
                                    stringResource(
                                        R.string.voice_result_ready,
                                        formatDurationMillis(session.processedDurationMs),
                                    )
                                } else {
                                    stringResource(R.string.voice_result_empty)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        VoiceResultTracks(
                            session = session,
                            onTogglePreviewTrack = onTogglePreviewTrack,
                            onPreviewTrackSeek = onPreviewTrackSeek,
                            onExportResult = onExportResult,
                            onShareOutput = onShareOutput,
                        )
                        OutlinedButton(
                            onClick = onClear,
                            enabled =
                                session.hasInputAudio ||
                                    session.hasProcessedAudio ||
                                    session.isLoadingInput ||
                                    session.isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.audio_action_clear))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }

    if (isLivePreviewDialogOpen) {
        AlertDialog(
            onDismissRequest = { isLivePreviewDialogOpen = false },
            title = {
                Text(text = stringResource(R.string.voice_live_preview_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.voice_live_preview_dialog_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hasShownLivePreviewDialog = true
                        isLivePreviewDialogOpen = false
                        onWorkflowModeSelected(VoiceWorkflowModeOption.Clip)
                    },
                    modifier = Modifier.testTag("voice-live-preview-use-clip"),
                ) {
                    Text(text = stringResource(R.string.voice_live_preview_dialog_use_clip))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hasShownLivePreviewDialog = true
                        isLivePreviewDialogOpen = false
                        onWorkflowModeSelected(VoiceWorkflowModeOption.Live)
                    },
                    modifier = Modifier.testTag("voice-live-preview-try-live"),
                ) {
                    Text(text = stringResource(R.string.voice_live_preview_dialog_try_live))
                }
            },
            modifier = Modifier.testTag("voice-live-preview-dialog"),
        )
    }
}

private val VoiceWorkflowSwitcherBarReservedHeight = 60.dp
