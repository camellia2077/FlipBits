package com.bag.audioandroid.ui.state

data class LibrarySelectionUiState(
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
) {
    val selectedCount: Int
        get() = selectedItemIds.size
}
