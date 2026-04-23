package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import com.bag.audioandroid.domain.SavedAudioItem

@Composable
internal fun LibraryTabScreenDialogs(
    renameTarget: SavedAudioItem?,
    renameValue: String,
    deleteTarget: SavedAudioItem?,
    showBulkDeleteDialog: Boolean,
    isSelectionMode: Boolean,
    filteredSelectedCount: Int,
    onRenameValueChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    onDismissBulkDelete: () -> Unit,
    onConfirmBulkDelete: () -> Unit,
) {
    renameTarget?.let { item ->
        RenameSavedAudioDialog(
            currentDisplayName = item.displayName,
            initialBaseName = renameValue.ifBlank { item.displayName.removeSuffix(".wav") },
            onDismiss = onDismissRename,
            onValueChange = onRenameValueChange,
            onConfirm = { onConfirmRename(item.itemId, renameValue) },
        )
    }

    deleteTarget?.let { item ->
        DeleteSavedAudioDialog(
            displayName = item.displayName,
            onDismiss = onDismissDelete,
            onConfirm = { onConfirmDelete(item.itemId) },
        )
    }

    if (isSelectionMode && showBulkDeleteDialog) {
        DeleteSelectedSavedAudioDialog(
            selectedCount = filteredSelectedCount,
            onDismiss = onDismissBulkDelete,
            onConfirm = onConfirmBulkDelete,
        )
    }
}
