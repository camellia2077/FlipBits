package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.LibrarySelectionUiState

@Composable
fun LibraryTabScreen(
    savedAudioItems: List<SavedAudioItem>,
    librarySelection: LibrarySelectionUiState,
    statusText: UiText,
    onImportAudio: () -> Unit,
    onSelectSavedAudio: (String) -> Unit,
    onEnterLibrarySelection: (String) -> Unit,
    onToggleLibrarySelection: (String) -> Unit,
    onSelectAllLibraryItems: (Collection<String>) -> Unit,
    onDeleteSelectedSavedAudio: () -> Unit,
    onClearLibrarySelection: () -> Unit,
    onDeleteSavedAudio: (String) -> Unit,
    onRenameSavedAudio: (String, String) -> Unit,
    onShareSavedAudio: (SavedAudioItem) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val screenState = rememberLibraryTabScreenState(savedAudioItems, librarySelection)
    val layoutDirection = LocalLayoutDirection.current

    LibraryTabScreenDialogs(
        renameTarget = screenState.renameTarget,
        renameValue = screenState.renameValue,
        deleteTarget = screenState.deleteTarget,
        showBulkDeleteDialog = screenState.showBulkDeleteDialog,
        isSelectionMode = librarySelection.isSelectionMode,
        filteredSelectedCount = screenState.filteredSelectedCount,
        onRenameValueChange = screenState.onRenameValueChange,
        onDismissRename = screenState.onDismissRename,
        onConfirmRename = { itemId, newName ->
            onRenameSavedAudio(itemId, newName)
            screenState.onRenameCompleted()
        },
        onDismissDelete = screenState.onDismissDelete,
        onConfirmDelete = { itemId ->
            onDeleteSavedAudio(itemId)
            screenState.onDeleteCompleted()
        },
        onDismissBulkDelete = screenState.onDismissBulkDelete,
        onConfirmBulkDelete = {
            onDeleteSelectedSavedAudio()
            screenState.onBulkDeleteCompleted()
        },
    )

    Column(
        modifier =
            modifier.padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryTabScreenContent(
            savedAudioItems = savedAudioItems,
            librarySelection = librarySelection,
            statusText = statusText,
            contentPadding = contentPadding,
            screenState = screenState,
            onImportAudio = onImportAudio,
            onSelectSavedAudio = onSelectSavedAudio,
            onEnterLibrarySelection = onEnterLibrarySelection,
            onToggleLibrarySelection = onToggleLibrarySelection,
            onSelectAllLibraryItems = onSelectAllLibraryItems,
            onClearLibrarySelection = onClearLibrarySelection,
            onShareSavedAudio = onShareSavedAudio,
        )
    }
}
