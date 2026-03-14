package com.bag.audioandroid.domain

sealed interface SavedAudioRenameResult {
    data class Success(
        val updatedItem: SavedAudioItem
    ) : SavedAudioRenameResult

    data object DuplicateName : SavedAudioRenameResult
    data object Failed : SavedAudioRenameResult
}

interface SavedAudioLibraryGateway {
    fun listSavedAudio(): List<SavedAudioItem>
    fun loadSavedAudio(itemId: String): SavedAudioContent?
    fun deleteSavedAudio(itemId: String): Boolean
    fun renameSavedAudio(itemId: String, newBaseName: String): SavedAudioRenameResult
}
