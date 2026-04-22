package com.bag.audioandroid.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryGateway
import com.bag.audioandroid.domain.SavedAudioRenameResult

class MediaStoreSavedAudioLibraryGateway(
    context: Context,
    audioIoGateway: AudioIoGateway,
) : SavedAudioLibraryGateway {
    private val contentResolver = context.contentResolver
    private val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val queries = MediaStoreSavedAudioQueries(contentResolver, collection)
    private val namingPolicy = SavedAudioFileNamingPolicy()
    private val metadataReader = SavedAudioMetadataReader(audioIoGateway)
    private val audioIoGateway = audioIoGateway

    override fun listSavedAudio(): List<SavedAudioItem> =
        queries.listRows().map { row ->
            metadataReader.toSavedAudioItem(
                row = row,
                metadata = metadataReader.readAudioMetadataHeader(contentResolver, row.uri),
                unknownModeWireName = UNKNOWN_MODE,
            )
        }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return null
        val uri = queries.uriForItemId(itemId) ?: return null
        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(fileBytes)
        if (!decoded.isWavSuccess ||
            decoded.wavStatusCode != AudioIoWavCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0
        ) {
            return null
        }

        return SavedAudioContent(
            item = savedAudioItem,
            pcm = decoded.pcm,
            sampleRateHz = decoded.sampleRateHz,
            metadata = decoded.metadata,
        )
    }

    override fun deleteSavedAudio(itemId: String): Boolean = queries.delete(itemId)

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return SavedAudioRenameResult.Failed
        val finalDisplayName = namingPolicy.normalizeRenameBaseName(newBaseName) ?: return SavedAudioRenameResult.Failed
        if (finalDisplayName == savedAudioItem.displayName) {
            return SavedAudioRenameResult.Success(savedAudioItem)
        }
        if (queries.displayNameExists(finalDisplayName)) {
            return SavedAudioRenameResult.DuplicateName
        }
        if (!queries.rename(itemId, finalDisplayName)) {
            return SavedAudioRenameResult.Failed
        }
        return findSavedAudioItem(itemId)
            ?.let { SavedAudioRenameResult.Success(it) }
            ?: SavedAudioRenameResult.Failed
    }

    override fun importAudio(uriString: String): SavedAudioImportResult {
        val sourceUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return SavedAudioImportResult.Failed
        val sourceBytes =
            contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return SavedAudioImportResult.Failed
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(sourceBytes)
        if (!decoded.isWavSuccess ||
            decoded.wavStatusCode != AudioIoWavCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0
        ) {
            return SavedAudioImportResult.UnsupportedFormat
        }

        val finalDisplayName =
            namingPolicy.nextAvailableDisplayName(
                preferredDisplayName =
                    namingPolicy.resolveImportDisplayName(
                        queries.resolveSourceDisplayName(sourceUri),
                    ),
                exists = queries::displayNameExists,
            )
        val insertedUri = queries.insertPendingAudio(finalDisplayName) ?: return SavedAudioImportResult.Failed

        val imported =
            runCatching {
                if (!queries.writeBytes(insertedUri, sourceBytes)) {
                    error("Failed to open destination output stream")
                }
                queries.markImportCompleted(insertedUri)
                val itemId = ContentUris.parseId(insertedUri).toString()
                findSavedAudioItem(itemId)
                    ?: metadataReader.importedFallbackItem(
                        itemId = itemId,
                        uri = insertedUri,
                        displayName = finalDisplayName,
                        metadata = decoded.metadata,
                        pcmSize = decoded.pcm.size,
                        sampleRateHz = decoded.sampleRateHz,
                        unknownModeWireName = UNKNOWN_MODE,
                    )
            }.getOrElse {
                contentResolver.delete(insertedUri, null, null)
                return SavedAudioImportResult.Failed
            }

        return SavedAudioImportResult.Success(imported)
    }

    private fun findSavedAudioItem(itemId: String): SavedAudioItem? =
        queries.findRow(itemId)?.let { row ->
            metadataReader.toSavedAudioItem(
                row = row,
                metadata = metadataReader.readAudioMetadataHeader(contentResolver, row.uri),
                unknownModeWireName = UNKNOWN_MODE,
            )
        }

    private companion object {
        const val UNKNOWN_MODE = "unknown"
    }
}
