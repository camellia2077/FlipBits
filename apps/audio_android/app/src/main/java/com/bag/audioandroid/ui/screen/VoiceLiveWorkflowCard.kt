package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.availablePresets
import com.bag.audioandroid.ui.state.VoiceSessionState

@Composable
internal fun VoiceLiveWorkflowCard(
    selectedThemeStyle: ThemeStyleOption,
    session: VoiceSessionState,
    onRequestRecordPermission: () -> Unit,
    onTrackModeSelected: (VoiceTrackModeOption) -> Unit,
    onPresetSelected: (VoiceFxPresetOption) -> Unit,
    onSubvoiceStyleSelected: (VoiceFxSubvoiceStyleOption) -> Unit,
    onStartLive: () -> Unit,
    onStopLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configControlsEnabled =
        !session.isRecording &&
            !session.isLoadingInput &&
            !session.isProcessing &&
            !session.isLiveActive

    VoiceSectionContainer(
        selectedThemeStyle = selectedThemeStyle,
        modifier = modifier.testTag("voice-live-workflow-card"),
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
            if (session.selectedTrackMode == VoiceTrackModeOption.Dual) {
                BinharicVoicingSelectorSection(
                    enabled = configControlsEnabled,
                    selectedStyle = session.selectedSubvoiceStyle,
                    onStyleSelected = onSubvoiceStyleSelected,
                )
            }
            Text(
                text = voiceStatusText(session),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("voice-live-status"),
            )
            LiveInputActions(
                session = session,
                onRequestRecordPermission = onRequestRecordPermission,
                onStartLive = onStartLive,
                onStopLive = onStopLive,
            )
            if (session.liveInputRouteLabel.isNotBlank() || session.liveOutputRouteLabel.isNotBlank()) {
                Text(
                    text = liveRouteText(session),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(R.string.voice_live_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
