package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.utilityActionIconButtonColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibrarySavedAudioRow(
    item: SavedAudioItem,
    isDecoded: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    folderName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onExportToFile: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onClearDecodeData: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (isSelectionMode) null else onLongClick,
                ).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        "${stringResource(SavedAudioModeFilter.labelResIdForModeWireName(item.modeWireName))} • " +
                            formatDurationMillis(item.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        if (isDecoded) {
                            stringResource(R.string.library_decode_status_decoded)
                        } else {
                            stringResource(R.string.library_decode_status_needed)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                folderName?.takeIf { it.isNotBlank() }?.let { resolvedFolderName ->
                    Text(
                        text = stringResource(R.string.library_folder_row_label, resolvedFolderName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!isSelectionMode) {
                IconButton(
                    onClick = { menuExpanded = true },
                    colors = utilityActionIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.library_action_more),
                    )
                }
                LibrarySavedAudioRowMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onExportToFile = onExportToFile,
                    onShare = onShare,
                    onMove = onMove,
                    onRename = onRename,
                    onClearDecodeData = onClearDecodeData,
                    canClearDecodeData = isDecoded,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun LibrarySavedAudioRowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onExportToFile: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onClearDecodeData: () -> Unit,
    canClearDecodeData: Boolean,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_export_to_file)) },
            leadingIcon = { Icon(Icons.Rounded.SaveAlt, contentDescription = null) },
            onClick = {
                onDismiss()
                onExportToFile()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_share)) },
            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                onShare()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_move)) },
            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
            onClick = {
                onDismiss()
                onMove()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_rename)) },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onRename()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_clear_decode_data)) },
            enabled = canClearDecodeData,
            onClick = {
                onDismiss()
                onClearDecodeData()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_action_delete)) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}
