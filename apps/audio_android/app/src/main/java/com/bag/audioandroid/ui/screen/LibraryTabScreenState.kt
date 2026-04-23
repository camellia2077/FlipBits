package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.state.LibrarySelectionUiState

@Composable
internal fun rememberLibraryTabScreenState(
    savedAudioItems: List<SavedAudioItem>,
    librarySelection: LibrarySelectionUiState,
): LibraryTabScreenState {
    var renameTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedAudioItem?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var showBulkDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedFilterName by rememberSaveable { mutableStateOf(SavedAudioModeFilter.All.name) }
    val selectedFilter = remember(selectedFilterName) { SavedAudioModeFilter.valueOf(selectedFilterName) }
    val filteredItems =
        remember(savedAudioItems, selectedFilter) {
            savedAudioItems.filter(selectedFilter::matches)
        }
    val filteredItemIds =
        remember(filteredItems) {
            filteredItems.map { it.itemId }
        }
    val filteredSelectedCount =
        remember(librarySelection.selectedItemIds, filteredItemIds) {
            librarySelection.selectedItemIds.intersect(filteredItemIds.toSet()).size
        }

    return remember(
        renameTarget,
        deleteTarget,
        renameValue,
        showBulkDeleteDialog,
        selectedFilter,
        filteredItems,
        filteredItemIds,
        filteredSelectedCount,
    ) {
        LibraryTabScreenState(
            renameTarget = renameTarget,
            deleteTarget = deleteTarget,
            renameValue = renameValue,
            showBulkDeleteDialog = showBulkDeleteDialog,
            selectedFilter = selectedFilter,
            filteredItems = filteredItems,
            filteredItemIds = filteredItemIds,
            filteredSelectedCount = filteredSelectedCount,
            onRenameStarted = { item ->
                renameTarget = item
                renameValue = item.displayName.removeSuffix(".wav")
            },
            onRenameValueChange = { renameValue = it },
            onDismissRename = {
                renameTarget = null
                renameValue = ""
            },
            onRenameCompleted = {
                renameTarget = null
                renameValue = ""
            },
            onDeleteStarted = { deleteTarget = it },
            onDismissDelete = { deleteTarget = null },
            onDeleteCompleted = { deleteTarget = null },
            onBulkDeleteRequested = { showBulkDeleteDialog = true },
            onDismissBulkDelete = { showBulkDeleteDialog = false },
            onBulkDeleteCompleted = { showBulkDeleteDialog = false },
            onFilterSelected = { filter ->
                selectedFilterName = filter.name
            },
        )
    }
}

@Immutable
internal data class LibraryTabScreenState(
    val renameTarget: SavedAudioItem?,
    val deleteTarget: SavedAudioItem?,
    val renameValue: String,
    val showBulkDeleteDialog: Boolean,
    val selectedFilter: SavedAudioModeFilter,
    val filteredItems: List<SavedAudioItem>,
    val filteredItemIds: List<String>,
    val filteredSelectedCount: Int,
    val onRenameStarted: (SavedAudioItem) -> Unit,
    val onRenameValueChange: (String) -> Unit,
    val onDismissRename: () -> Unit,
    val onRenameCompleted: () -> Unit,
    val onDeleteStarted: (SavedAudioItem) -> Unit,
    val onDismissDelete: () -> Unit,
    val onDeleteCompleted: () -> Unit,
    val onBulkDeleteRequested: () -> Unit,
    val onDismissBulkDelete: () -> Unit,
    val onBulkDeleteCompleted: () -> Unit,
    val onFilterSelected: (SavedAudioModeFilter) -> Unit,
)
