package com.bag.audioandroid.data

import android.content.Context
import android.net.Uri
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
        metadata: GeneratedAudioMetadata,
    ): Uri? =
        runCatching {
            val shareDirectory = File(appContext.cacheDir, SharedAudioDirectoryName).apply { mkdirs() }
            shareDirectory.listFiles()?.forEach(File::delete)
            val shareFile = File(shareDirectory, displayName)
            FileOutputStream(shareFile).use { outputStream ->
                if (pcm.isNotEmpty()) {
                    val wavBytes = audioIoGateway.encodeMonoPcm16ToWavBytes(sampleRateHz, pcm, metadata)
                    if (wavBytes.isEmpty()) {
                        return null
                    }
                    outputStream.write(wavBytes)
                } else {
                    writeMonoPcm16WavFromFile(
                        output = outputStream,
                        pcmFilePath = requireNotNull(pcmFilePath),
                        sampleRateHz = sampleRateHz,
                        totalSamples = metadata.pcmSampleCount,
                        metadata = metadata,
                    )
                }
                outputStream.flush()
            }
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                shareFile,
            )
        }.getOrNull()

    private companion object {
        const val SharedAudioDirectoryName = "shared_audio"
    }
}
