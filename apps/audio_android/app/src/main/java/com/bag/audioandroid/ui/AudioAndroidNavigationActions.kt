package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.state.PlayerShellEvent
import com.bag.audioandroid.ui.state.QueueSheetValue
import com.bag.audioandroid.ui.state.SnackbarMessage
import com.bag.audioandroid.ui.state.reduce
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
        uiState.update { state ->
            state.copy(
                playerShellState = state.playerShellState.reduce(PlayerShellEvent.OpenExpandedPlayer),
            )
        }
    }

    fun onClosePlayerDetailSheet() {
        uiState.update { state ->
            state.copy(
                playerShellState = state.playerShellState.reduce(PlayerShellEvent.CollapseExpandedPlayer),
            )
        }
    }

    fun onQueueSheetValueChanged(value: QueueSheetValue) {
        uiState.update { state ->
            state.copy(
                playerShellState =
                    state.playerShellState.reduce(
                        PlayerShellEvent.SetQueueValue(value = value),
                    ),
            )
        }
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

    fun showSnackbar(
        text: UiText,
        durationMillis: Long? = null,
    ) {
        uiState.update { state ->
            state.copy(
                snackbarMessage =
                    SnackbarMessage(
                        id = System.currentTimeMillis(),
                        text = text,
                        durationMillis = durationMillis,
                    ),
            )
        }
    }
}
