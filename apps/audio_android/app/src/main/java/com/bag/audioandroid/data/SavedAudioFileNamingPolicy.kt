package com.bag.audioandroid.data

internal class SavedAudioFileNamingPolicy {
    fun normalizeRenameBaseName(newBaseName: String): String? = newBaseName.trim().takeIf { it.isNotEmpty() }?.let(::ensureWavExtension)

    fun resolveImportDisplayName(queriedDisplayName: String?): String {
        val rawName =
            queriedDisplayName
                ?.substringAfterLast('/')
                ?.trim()
                .orEmpty()
                .ifBlank { "imported_audio" }
        return ensureWavExtension(rawName)
    }

    fun nextAvailableDisplayName(
        preferredDisplayName: String,
        exists: (String) -> Boolean,
    ): String {
        if (!exists(preferredDisplayName)) {
            return preferredDisplayName
        }
        val baseName = preferredDisplayName.removeSuffix(".wav")
        var counter = 1
        while (true) {
            val candidate = "$baseName ($counter).wav"
            if (!exists(candidate)) {
                return candidate
            }
            counter += 1
        }
    }

    private fun ensureWavExtension(baseName: String): String =
        if (baseName.endsWith(".wav", ignoreCase = true)) {
            baseName
        } else {
            "$baseName.wav"
        }
}
