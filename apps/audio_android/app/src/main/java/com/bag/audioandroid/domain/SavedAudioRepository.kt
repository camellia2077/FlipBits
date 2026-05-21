package com.bag.audioandroid.domain

interface SavedAudioRepository {
    fun suggestGeneratedAudioDisplayName(
        inputText: String,
        metadata: GeneratedAudioMetadata,
    ): String

    fun exportGeneratedAudio(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult

    fun exportGeneratedAudioToDocument(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean

    fun listSavedAudio(): List<SavedAudioItem>

    fun loadSavedAudio(itemId: String): SavedAudioContent?

    fun deleteSavedAudio(itemId: String): Boolean

    fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult

    fun importAudio(uriString: String): SavedAudioImportResult

    fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean

    fun shareSavedAudio(item: SavedAudioItem): Boolean

    fun shareAudio(
        displayName: String,
        uriString: String,
    ): Boolean = false

    fun readLibraryMetadata(): SavedAudioLibraryMetadata = SavedAudioLibraryMetadata()

    fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

    fun renameSavedAudioFolder(
        folderId: String,
        name: String,
    ): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

    fun deleteSavedAudioFolder(folderId: String): Boolean = false

    fun assignSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean = false
}
