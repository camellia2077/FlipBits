package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.availablePresets
import com.bag.audioandroid.ui.model.usesSubvoiceStyle
import com.bag.audioandroid.ui.state.VoiceSessionState

@Composable
internal fun VoiceClipWorkflowCard(
    selectedThemeStyle: ThemeStyleOption,
    session: VoiceSessionState,
    onRequestRecordPermission: () -> Unit,
    onTrackModeSelected: (VoiceTrackModeOption) -> Unit,
    onInputSourceSelected: (VoiceInputSourceOption) -> Unit,
    onRecordProcessingModeSelected: (VoiceRecordProcessingModeOption) -> Unit,
    onPresetSelected: (VoiceFxPresetOption) -> Unit,
    onSubvoiceStyleSelected: (VoiceFxSubvoiceStyleOption) -> Unit,
    onImportAudio: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onProcess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configControlsEnabled = !session.isRecording && !session.isLoadingInput && !session.isProcessing

    VoiceSectionContainer(
        selectedThemeStyle = selectedThemeStyle,
        modifier = modifier.testTag("voice-clip-workflow-card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VoiceTrackModeSelectorSection(
                enabled = configControlsEnabled,
                selectedTrackMode = session.selectedTrackMode,
                onTrackModeSelected = onTrackModeSelected,
            )
            VoicePresetSelectorSection(
                enabled = configControlsEnabled,
                availablePresets = session.selectedTrackMode.availablePresets(),
                selectedPreset = session.selectedPreset,
                onPresetSelected = onPresetSelected,
            )
            Text(
                text = stringResource(session.selectedPreset.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (session.selectedTrackMode.usesSubvoiceStyle) {
                VoiceSubvoiceStyleSelectorSection(
                    enabled = configControlsEnabled,
                    selectedStyle = session.selectedSubvoiceStyle,
                    onStyleSelected = onSubvoiceStyleSelected,
                )
            }
            Text(
                text = stringResource(R.string.voice_input_source_title),
                style = MaterialTheme.typography.titleSmall,
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                VoiceInputSourceOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == session.selectedInputSource,
                        onClick = { onInputSourceSelected(option) },
                        enabled =
                            !session.isRecording &&
                                !session.isLoadingInput &&
                                !session.isProcessing &&
                                !session.isLiveActive,
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = VoiceInputSourceOption.entries.size,
                            ),
                        colors = appSegmentedButtonColors(),
                        label = { Text(text = stringResource(option.labelResId)) },
                    )
                }
            }
            if (session.selectedInputSource == VoiceInputSourceOption.Record) {
                VoiceRecordProcessingModeSection(
                    selectedMode = session.selectedRecordProcessingMode,
                    enabled = configControlsEnabled && !session.isLiveActive,
                    onModeSelected = onRecordProcessingModeSelected,
                )
                RecordInputActions(
                    session = session,
                    onRequestRecordPermission = onRequestRecordPermission,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                )
            } else {
                Button(
                    onClick = onImportAudio,
                    enabled = session.canImportAudio,
                    modifier = Modifier.fillMaxWidth().testTag("voice-upload-button"),
                ) {
                    Text(text = stringResource(R.string.voice_action_choose_upload))
                }
            }
            Text(
                text = voiceStatusText(session),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("voice-clip-status"),
            )
            if (session.hasInputAudio &&
                !session.hasProcessedAudio &&
                !session.isRecording &&
                !session.isLoadingInput
            ) {
                Text(
                    text = inputReadyText(session),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            VoiceProcessAction(
                session = session,
                onProcess = onProcess,
            )
        }
    }
}

@Composable
private fun VoiceProcessAction(
    session: VoiceSessionState,
    onProcess: () -> Unit,
) {
    val buttonText =
        stringResource(
            if (session.isProcessing) {
                R.string.voice_action_process_busy
            } else {
                R.string.voice_action_process
            },
        )
    val buttonModifier = Modifier.fillMaxWidth().testTag("voice-process-button")
    if (session.hasProcessedAudio && !session.isProcessing) {
        OutlinedButton(
            onClick = onProcess,
            enabled = session.canProcess,
            modifier = buttonModifier,
        ) {
            Text(text = buttonText)
        }
    } else {
        Button(
            onClick = onProcess,
            enabled = session.canProcess,
            modifier = buttonModifier,
        ) {
            Text(text = buttonText)
        }
    }
    voiceProcessHintText(session)?.let { hint ->
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("voice-process-hint"),
        )
    }
}
