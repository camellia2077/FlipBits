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
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption

@Composable
internal fun VoiceWorkflowSelectorSection(
    selectedWorkflowMode: VoiceWorkflowModeOption,
    onWorkflowModeSelected: (VoiceWorkflowModeOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        VoiceWorkflowModeOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selectedWorkflowMode,
                onClick = { onWorkflowModeSelected(option) },
                enabled = enabled,
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = VoiceWorkflowModeOption.entries.size,
                    ),
                colors = appSegmentedButtonColors(),
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("voice-workflow-${option.id}"),
                label = {
                    val isSelected = selectedWorkflowMode == option
                    Text(
                        text = stringResource(option.labelResId),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    }
}
