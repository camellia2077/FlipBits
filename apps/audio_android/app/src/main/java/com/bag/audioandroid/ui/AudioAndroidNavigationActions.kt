package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidNavigationActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
) {
    fun onTabSelected(
        tab: AppTab,
        refreshSavedAudioItems: () -> Unit,
    ) {
        if (tab == AppTab.Library) {
            refreshSavedAudioItems()
        }
        uiState.update { state ->
            state.copy(
                selectedTab = tab,
                librarySelection =
                    if (tab == AppTab.Library) {
                        state.librarySelection
                    } else {
                        LibrarySelectionUiState()
                    },
            )
        }
    }

    fun onOpenAboutPage() {
        uiState.update { it.copy(showAboutPage = true) }
    }

    fun onCloseAboutPage() {
        uiState.update { it.copy(showAboutPage = false) }
    }

    fun onOpenLicensesPage() {
        uiState.update { it.copy(showLicensesPage = true, showAboutPage = false) }
    }

    fun onCloseLicensesPage() {
        uiState.update { it.copy(showLicensesPage = false, showAboutPage = true) }
    }

    fun onOpenPlayerDetailSheet() {
        uiState.update { it.copy(showPlayerDetailSheet = true, showSavedAudioSheet = false) }
    }

    fun onClosePlayerDetailSheet() {
        uiState.update { it.copy(showPlayerDetailSheet = false) }
    }

    fun onSnackbarMessageShown(messageId: Long) {
        uiState.update { state ->
            if (state.snackbarMessage?.id == messageId) {
                state.copy(snackbarMessage = null)
            } else {
                state
            }
        }
    }
}
