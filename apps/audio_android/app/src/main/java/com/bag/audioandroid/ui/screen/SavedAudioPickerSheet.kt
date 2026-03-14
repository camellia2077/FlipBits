package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter

@Composable
internal fun SavedAudioPickerSheet(
    savedAudioItems: List<SavedAudioItem>,
    selectedFilter: SavedAudioModeFilter,
    onFilterSelected: (SavedAudioModeFilter) -> Unit,
    onSavedAudioSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredItems = savedAudioItems.filter(selectedFilter::matches)

    Column(
        modifier = modifier.padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.audio_saved_audio_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (savedAudioItems.isNotEmpty()) {
            SavedAudioModeFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        if (savedAudioItems.isEmpty()) {
            Text(
                text = stringResource(R.string.audio_saved_audio_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else if (filteredItems.isEmpty()) {
            Text(
                text = stringResource(R.string.saved_audio_filter_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(filteredItems, key = { it.itemId }) { item ->
                    ListItem(
                        headlineContent = { Text(item.displayName) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${stringResource(SavedAudioModeFilter.labelResIdForModeWireName(item.modeWireName))} • " +
                                        formatDurationMillis(item.durationMs)
                                )
                                Text(text = formatSavedAudioTime(item.savedAtEpochSeconds))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSavedAudioSelected(item.itemId) }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
