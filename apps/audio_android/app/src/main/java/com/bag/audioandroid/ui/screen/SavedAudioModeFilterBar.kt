package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bag.audioandroid.ui.model.SavedAudioModeFilter

@Composable
internal fun SavedAudioModeFilterBar(
    selectedFilter: SavedAudioModeFilter,
    onFilterSelected: (SavedAudioModeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        SavedAudioModeFilter.entries.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = SavedAudioModeFilter.entries.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                ),
                modifier = Modifier.weight(1f),
                label = {
                    Text(stringResource(filter.labelResId))
                }
            )
        }
    }
}
