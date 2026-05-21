package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.savedAudioPickerColors

@Composable
internal fun SavedAudioPickerSheet(
    savedAudioItems: List<SavedAudioItem>,
    currentItemId: String?,
    selectedFilter: SavedAudioModeFilter,
    onFilterSelected: (SavedAudioModeFilter) -> Unit,
    onSavedAudioSelected: (String) -> Unit,
    maxListHeight: Dp = 360.dp,
    sheetHeight: Dp? = null,
    onCollapseRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val filteredItems = savedAudioItems.filter(selectedFilter::matches)
    val pickerColors = savedAudioPickerColors()

    LaunchedEffect(savedAudioItems.size, filteredItems.size, currentItemId, selectedFilter) {
        debugSavedAudioPickerLog(
            "sheetState",
            "savedItems=${savedAudioItems.size} filteredItems=${filteredItems.size} " +
                "currentItemId=${currentItemId.orEmpty()} filter=${selectedFilter.name}",
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = pickerColors.sheetContainer,
        contentColor = pickerColors.sheetContent,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .then(
                        if (sheetHeight != null) {
                            Modifier.height(sheetHeight)
                        } else {
                            Modifier
                        },
                    ).onGloballyPositioned { coordinates ->
                        val position = coordinates.boundsInRoot().topLeft
                        debugSavedAudioPickerLog(
                            "sheetLayout",
                            "x=${position.x.toInt()} y=${position.y.toInt()} " +
                                "w=${coordinates.size.width} h=${coordinates.size.height}",
                        )
                    },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(enabled = onCollapseRequested != null) {
                                onCollapseRequested?.invoke()
                            }.align(androidx.compose.ui.Alignment.Center)
                            .fillMaxWidth(0.12f)
                            .background(pickerColors.supportingTextColor.copy(alpha = 0.42f))
                            .height(4.dp),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = sheetHeight != null)
                        .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.audio_saved_audio_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = pickerColors.titleColor,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                if (savedAudioItems.isNotEmpty()) {
                    SavedAudioModeFilterBar(
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                if (savedAudioItems.isEmpty()) {
                    Text(
                        text = stringResource(R.string.audio_saved_audio_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = pickerColors.supportingTextColor,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                } else if (filteredItems.isEmpty()) {
                    Text(
                        text = stringResource(R.string.saved_audio_filter_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = pickerColors.supportingTextColor,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = sheetHeight != null)
                                .heightIn(max = maxListHeight),
                    ) {
                        items(filteredItems, key = { it.itemId }) { item ->
                            val isSelected = item.itemId == currentItemId
                            ListItem(
                                headlineContent = { Text(item.displayName.displayStem()) },
                                colors = if (isSelected) pickerColors.selectedItemColors else pickerColors.unselectedItemColors,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onSavedAudioSelected(item.itemId) }
                                        .padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun debugSavedAudioPickerLog(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d("SavedAudioPickerDiag", "$label $message")
    } catch (_: RuntimeException) {
    }
}

private fun String.displayStem(): String {
    if (endsWith(".wav", ignoreCase = true)) {
        return dropLast(4)
    }
    val lastDotIndex = lastIndexOf('.')
    return if (lastDotIndex > 0) substring(0, lastDotIndex) else this
}
