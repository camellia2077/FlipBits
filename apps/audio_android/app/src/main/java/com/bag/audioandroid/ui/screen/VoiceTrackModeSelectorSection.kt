package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.VoiceTrackModeOption

@Composable
internal fun VoiceTrackModeSelectorSection(
    enabled: Boolean,
    selectedTrackMode: VoiceTrackModeOption,
    onTrackModeSelected: (VoiceTrackModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth().testTag("voice-track-mode-selector"),
    ) {
        VoiceTrackModeOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selectedTrackMode,
                onClick = { onTrackModeSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = VoiceTrackModeOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                modifier = Modifier.testTag("voice-track-mode-${option.id}"),
                label = { Text(text = stringResource(option.labelResId)) },
            )
        }
    }
}
