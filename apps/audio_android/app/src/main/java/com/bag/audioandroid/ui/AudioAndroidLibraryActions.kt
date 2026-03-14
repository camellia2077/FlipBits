package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class AudioAndroidLibraryActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val savedAudioRepository: SavedAudioRepository,
    private val stopPlayback: () -> Unit
) {
    fun onSavedAudioSelected(itemId: String) {
        stopPlayback()
        if (!prepareSavedAudioSelection(itemId, switchToAudioTab = true, clearLibrarySelection = true)) {
            uiState.update { it.copy(showSavedAudioSheet = false) }
            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
            return
        }
        val savedAudio = uiState.value.selectedSavedAudio ?: return
        setCurrentStatusText(
            UiText.Resource(
                R.string.status_saved_audio_loaded,
                listOf(savedAudio.item.displayName)
            )
        )
    }

    fun prepareSavedAudioSelection(
        itemId: String,
        switchToAudioTab: Boolean = false,
        clearLibrarySelection: Boolean = false
    ): Boolean {
        val savedAudio = savedAudioRepository.loadSavedAudio(itemId) ?: return false
        uiState.update { state ->
            state.copy(
                selectedTab = if (switchToAudioTab) AppTab.Audio else state.selectedTab,
                showSavedAudioSheet = if (switchToAudioTab) false else state.showSavedAudioSheet,
                currentPlaybackSource = AudioPlaybackSource.Saved(savedAudio.item.itemId),
                selectedSavedAudio = SavedAudioPlaybackSelection(
                    item = savedAudio.item,
                    pcm = savedAudio.pcm,
                    sampleRateHz = savedAudio.sampleRateHz,
                    playback = playbackRuntimeGateway.load(savedAudio.pcm.size, savedAudio.sampleRateHz)
                ),
                librarySelection = if (clearLibrarySelection) {
                    LibrarySelectionUiState()
                } else {
                    state.librarySelection
                }
            )
        }
        return true
    }

    fun onEnterLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (state.savedAudioItems.none { it.itemId == itemId }) {
                return@update state
            }
            state.copy(
                librarySelection = LibrarySelectionUiState(
                    isSelectionMode = true,
                    selectedItemIds = setOf(itemId)
                )
            )
        }
    }

    fun onToggleLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (!state.librarySelection.isSelectionMode ||
                state.savedAudioItems.none { it.itemId == itemId }) {
                return@update state
            }

            val updatedSelection = if (itemId in state.librarySelection.selectedItemIds) {
                state.librarySelection.selectedItemIds - itemId
            } else {
                state.librarySelection.selectedItemIds + itemId
            }
            state.copy(
                librarySelection = if (updatedSelection.isEmpty()) {
                    LibrarySelectionUiState()
                } else {
                    state.librarySelection.copy(selectedItemIds = updatedSelection)
                }
            )
        }
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>? = null) {
        uiState.update { state ->
            val allItemIds = itemIds
                ?.filter { candidateId -> state.savedAudioItems.any { it.itemId == candidateId } }
                ?.toSet()
                ?: state.savedAudioItems.map { it.itemId }.toSet()
            if (allItemIds.isEmpty()) {
                state
            } else {
                state.copy(
                    librarySelection = LibrarySelectionUiState(
                        isSelectionMode = true,
                        selectedItemIds = allItemIds
                    )
                )
            }
        }
    }

    fun onClearLibrarySelection() {
        uiState.update { it.copy(librarySelection = LibrarySelectionUiState()) }
    }

    fun onDeleteSelectedSavedAudio() {
        val selectedItemIds = uiState.value.librarySelection.selectedItemIds.toList()
        if (selectedItemIds.isEmpty()) {
            return
        }
        if (selectedItemIds.size == 1) {
            onDeleteSavedAudio(selectedItemIds.first())
            return
        }

        val deletedItemIds = selectedItemIds.filterTo(mutableSetOf()) { itemId ->
            savedAudioRepository.deleteSavedAudio(itemId)
        }
        if (deletedItemIds.isEmpty()) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_delete_failed))
            }
            return
        }

        stopPlaybackIfCurrentSavedAudio(deletedItemIds)
        refreshSavedAudioItems()
        uiState.update { state ->
            state.copy(
                libraryStatusText = if (deletedItemIds.size == selectedItemIds.size) {
                    UiText.Resource(
                        R.string.library_status_deleted_multiple,
                        listOf(deletedItemIds.size)
                    )
                } else {
                    UiText.Resource(
                        R.string.library_status_delete_partial,
                        listOf(deletedItemIds.size, selectedItemIds.size)
                    )
                }
            )
        }
    }

    fun onDeleteSavedAudio(itemId: String) {
        val wasDeleted = savedAudioRepository.deleteSavedAudio(itemId)
        if (!wasDeleted) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_delete_failed))
            }
            return
        }

        stopPlaybackIfCurrentSavedAudio(itemId)
        refreshSavedAudioItems()
        uiState.update {
            it.copy(libraryStatusText = UiText.Resource(R.string.library_status_deleted))
        }
    }

    fun onRenameSavedAudio(itemId: String, newBaseName: String) {
        when (val result = savedAudioRepository.renameSavedAudio(itemId, newBaseName)) {
            is SavedAudioRenameResult.Success -> {
                refreshSavedAudioItems()
                uiState.update { state ->
                    val selectedSavedAudio = state.selectedSavedAudio
                    val updatedSelection = if (selectedSavedAudio?.item?.itemId == itemId) {
                        selectedSavedAudio.copy(item = result.updatedItem)
                    } else {
                        selectedSavedAudio
                    }
                    state.copy(
                        selectedSavedAudio = updatedSelection,
                        libraryStatusText = UiText.Resource(
                            R.string.library_status_renamed,
                            listOf(result.updatedItem.displayName)
                        )
                    )
                }
            }

            SavedAudioRenameResult.DuplicateName -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_rename_duplicate))
            }

            SavedAudioRenameResult.Failed -> uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_rename_failed))
            }
        }
    }

    fun onShareCurrentSavedAudio() {
        val currentSavedAudio = uiState.value.currentSavedAudioItem ?: return
        if (!savedAudioRepository.shareSavedAudio(currentSavedAudio)) {
            setCurrentStatusText(UiText.Resource(R.string.library_status_share_failed))
        }
    }

    fun onShareSavedAudio(item: SavedAudioItem) {
        if (!savedAudioRepository.shareSavedAudio(item)) {
            uiState.update {
                it.copy(libraryStatusText = UiText.Resource(R.string.library_status_share_failed))
            }
        }
    }

    fun refreshSavedAudioItems() {
        val savedAudioItems = savedAudioRepository.listSavedAudio()
        val savedAudioItemIds = savedAudioItems.map { it.itemId }.toSet()
        uiState.update { state ->
            val selectedItemIds = state.librarySelection.selectedItemIds.intersect(savedAudioItemIds)
            val currentPlaybackSource = state.currentPlaybackSource
            state.copy(
                savedAudioItems = savedAudioItems,
                selectedSavedAudio = state.selectedSavedAudio?.takeIf { it.item.itemId in savedAudioItemIds },
                currentPlaybackSource = if (currentPlaybackSource is AudioPlaybackSource.Saved &&
                    currentPlaybackSource.itemId !in savedAudioItemIds) {
                    AudioPlaybackSource.Generated(state.transportMode)
                } else {
                    currentPlaybackSource
                },
                librarySelection = if (selectedItemIds.isEmpty()) {
                    LibrarySelectionUiState()
                } else {
                    state.librarySelection.copy(selectedItemIds = selectedItemIds)
                }
            )
        }
    }

    private fun stopPlaybackIfCurrentSavedAudio(itemId: String) {
        stopPlaybackIfCurrentSavedAudio(setOf(itemId))
    }

    private fun stopPlaybackIfCurrentSavedAudio(itemIds: Set<String>) {
        val currentSource = uiState.value.currentPlaybackSource
        if (currentSource is AudioPlaybackSource.Saved && currentSource.itemId in itemIds) {
            stopPlayback()
        }
    }

    private fun setCurrentStatusText(statusText: UiText) {
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = statusText)
        }
    }
}
