package com.bag.audioandroid.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.bag.audioandroid.domain.AudioIoCodes
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryGateway
import com.bag.audioandroid.domain.SavedAudioRenameResult

class MediaStoreSavedAudioLibraryGateway(
    context: Context,
    private val audioIoGateway: AudioIoGateway
) : SavedAudioLibraryGateway {
    private val contentResolver = context.contentResolver
    private val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    override fun listSavedAudio(): List<SavedAudioItem> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(RELATIVE_PATH_PREFIX)
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return buildList {
            contentResolver.query(collection, projection, selection, args, sortOrder)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val displayName = cursor.getString(nameIndex).orEmpty()
                    add(
                        SavedAudioItem(
                            itemId = id.toString(),
                            displayName = displayName,
                            uriString = ContentUris.withAppendedId(collection, id).toString(),
                            modeWireName = parseModeWireName(displayName),
                            durationMs = cursor.getLong(durationIndex).coerceAtLeast(0L),
                            savedAtEpochSeconds = cursor.getLong(dateAddedIndex).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }
    }

    override fun loadSavedAudio(itemId: String): SavedAudioContent? {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return null
        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return null)
        val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val decoded = audioIoGateway.decodeMonoPcm16WavBytes(fileBytes)
        if (!decoded.isSuccess ||
            decoded.statusCode != AudioIoCodes.STATUS_OK ||
            decoded.channels != 1 ||
            decoded.sampleRateHz <= 0) {
            return null
        }

        return SavedAudioContent(
            item = savedAudioItem,
            pcm = decoded.pcm,
            sampleRateHz = decoded.sampleRateHz
        )
    }

    override fun deleteSavedAudio(itemId: String): Boolean {
        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return false)
        return runCatching { contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
    }

    override fun renameSavedAudio(itemId: String, newBaseName: String): SavedAudioRenameResult {
        val savedAudioItem = findSavedAudioItem(itemId) ?: return SavedAudioRenameResult.Failed
        val normalizedBaseName = newBaseName.trim()
        if (normalizedBaseName.isEmpty()) {
            return SavedAudioRenameResult.Failed
        }
        val finalDisplayName = ensureWavExtension(normalizedBaseName)
        if (finalDisplayName == savedAudioItem.displayName) {
            return SavedAudioRenameResult.Success(savedAudioItem)
        }
        if (displayNameExists(finalDisplayName)) {
            return SavedAudioRenameResult.DuplicateName
        }

        val uri = ContentUris.withAppendedId(collection, itemId.toLongOrNull() ?: return SavedAudioRenameResult.Failed)
        val updated = runCatching {
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.DISPLAY_NAME, finalDisplayName) },
                null,
                null
            ) > 0
        }.getOrDefault(false)
        if (!updated) {
            return SavedAudioRenameResult.Failed
        }
        return findSavedAudioItem(itemId)
            ?.let { SavedAudioRenameResult.Success(it) }
            ?: SavedAudioRenameResult.Failed
    }

    private fun findSavedAudioItem(itemId: String): SavedAudioItem? =
        listSavedAudio().firstOrNull { it.itemId == itemId }

    private fun displayNameExists(displayName: String): Boolean {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(displayName, RELATIVE_PATH_PREFIX)
        contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun ensureWavExtension(baseName: String): String =
        if (baseName.endsWith(".wav", ignoreCase = true)) {
            baseName
        } else {
            "$baseName.wav"
        }

    private fun parseModeWireName(displayName: String): String {
        MODE_REGEX.find(displayName)?.groupValues?.getOrNull(1)?.let { return it }
        return UNKNOWN_MODE
    }

    private companion object {
        const val RELATIVE_PATH_PREFIX = "Music/WaveBits%"
        const val UNKNOWN_MODE = "unknown"
        val MODE_REGEX = Regex("_(flash|pro|ultra)_\\d{8}_\\d{6}(?:_\\d+)?\\.wav$", RegexOption.IGNORE_CASE)
    }
}
