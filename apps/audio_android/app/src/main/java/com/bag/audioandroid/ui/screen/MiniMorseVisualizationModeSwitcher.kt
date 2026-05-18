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
import com.bag.audioandroid.ui.playerSegmentedButtonColors

@Composable
internal fun MiniMorseVisualizationModeSwitcher(
    selectedMode: MiniMorseVisualizationMode,
    onModeSelected: (MiniMorseVisualizationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModes =
        listOf(
            MiniMorseVisualizationMode.Horizontal,
            MiniMorseVisualizationMode.Vertical,
        )
    SingleChoiceSegmentedButtonRow(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("morse-visualization-mode-switcher"),
    ) {
        displayModes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = displayModes.size,
                    ),
                colors = playerSegmentedButtonColors(),
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("morse-visualization-mode-${mode.name.lowercase()}"),
                label = {
                    Text(text = stringResource(mode.labelResId))
                },
            )
        }
    }
}
