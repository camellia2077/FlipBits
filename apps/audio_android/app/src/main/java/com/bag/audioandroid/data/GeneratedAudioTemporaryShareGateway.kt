package com.bag.audioandroid.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.bag.audioandroid.domain.AudioIoGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import java.io.File
import java.io.FileOutputStream

internal class GeneratedAudioTemporaryShareGateway(
    private val appContext: Context,
    private val audioIoGateway: AudioIoGateway,
) {
    fun createShareUri(
        displayName: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata?,
    ): Uri? =
        runCatching {
            logShareDebug(
                "create share uri start displayName=$displayName pcmSamples=${pcm.size} " +
                    "pcmFilePathPresent=${!pcmFilePath.isNullOrBlank()} sampleRateHz=$sampleRateHz " +
                    "metadataPresent=${metadata != null} metadataSamples=${metadata?.pcmSampleCount ?: -1}",
            )
            val shareDirectory = File(appContext.cacheDir, SharedAudioDirectoryName).apply { mkdirs() }
            shareDirectory.listFiles()?.forEach(File::delete)
            val shareFile = File(shareDirectory, displayName)
            FileOutputStream(shareFile).use { outputStream ->
                if (pcm.isNotEmpty()) {
                    val wavBytes = audioIoGateway.encodeMonoPcm16ToWavBytes(sampleRateHz, pcm, metadata)
                    if (wavBytes.isEmpty()) {
                        logShareFailure(
                            "create share uri failed: WAV encode returned empty displayName=$displayName " +
                                "pcmSamples=${pcm.size} sampleRateHz=$sampleRateHz",
                        )
                        return null
                    }
                    outputStream.write(wavBytes)
                } else {
                    if (pcmFilePath.isNullOrBlank()) {
                        logShareFailure("create share uri failed: no pcm and no pcmFilePath displayName=$displayName")
                        return null
                    }
                    val fileMetadata =
                        metadata ?: run {
                            logShareFailure("create share uri failed: no metadata for pcmFilePath displayName=$displayName")
                            return null
                        }
                    writeMonoPcm16WavFromFile(
                        output = outputStream,
                        pcmFilePath = pcmFilePath,
                        sampleRateHz = sampleRateHz,
                        totalSamples = fileMetadata.pcmSampleCount,
                        metadata = fileMetadata,
                    )
                }
                outputStream.flush()
            }
            FileProvider
                .getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    shareFile,
                ).also { uri ->
                    logShareDebug(
                        "create share uri success displayName=$displayName filePath=${shareFile.absolutePath} " +
                            "fileBytes=${shareFile.length()} uri=$uri",
                    )
                }
        }.getOrElse { throwable ->
            logShareFailure("create share uri threw displayName=$displayName", throwable)
            null
        }

    private companion object {
        const val VoiceExportTag = "VoiceExportDiag"
        const val SharedAudioDirectoryName = "shared_audio"

        fun logShareDebug(message: String) {
            try {
                Log.d(VoiceExportTag, message)
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
            }
        }

        fun logShareFailure(
            message: String,
            throwable: Throwable? = null,
        ) {
            try {
                if (throwable == null) {
                    Log.e(VoiceExportTag, message)
                } else {
                    Log.e(VoiceExportTag, message, throwable)
                }
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
            }
        }
    }
}
