package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption

@Composable
internal fun VoiceRecordProcessingModeSection(
    selectedMode: VoiceRecordProcessingModeOption,
    enabled: Boolean,
    onModeSelected: (VoiceRecordProcessingModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.voice_record_processing_mode_title),
        style = MaterialTheme.typography.titleSmall,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth().testTag("voice-record-processing-mode-selector"),
    ) {
        VoiceRecordProcessingModeOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selectedMode,
                onClick = { onModeSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = VoiceRecordProcessingModeOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                modifier = Modifier.weight(1f).testTag("voice-record-processing-mode-${option.id}"),
                label = { Text(text = stringResource(option.labelResId)) },
            )
        }
    }
}
