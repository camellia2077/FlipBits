package com.bag.audioandroid.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioShareGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioItem

internal class AndroidIntentAudioShareGateway(
    private val appContext: Context,
    private val generatedAudioTemporaryShareGateway: GeneratedAudioTemporaryShareGateway,
) : AudioShareGateway {
    override fun shareSavedAudio(item: SavedAudioItem): Boolean =
        shareAudio(
            displayName = item.displayName,
            uriString = item.uriString,
        )

    override fun shareGeneratedAudio(
        displayName: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): Boolean {
        logShareDebug(
            "share generated start displayName=$displayName pcmSamples=${pcm.size} " +
                "pcmFilePathPresent=${!pcmFilePath.isNullOrBlank()} sampleRateHz=$sampleRateHz " +
                "metadataSamples=${metadata.pcmSampleCount}",
        )
        val shareUri =
            generatedAudioTemporaryShareGateway
                .createShareUri(
                    displayName = displayName,
                    pcm = pcm,
                    pcmFilePath = pcmFilePath,
                    sampleRateHz = sampleRateHz,
                    metadata = metadata,
                )
        if (shareUri == null) {
            logShareFailure("share generated failed: createShareUri returned null displayName=$displayName")
            return false
        }
        return shareAudio(displayName, shareUri.toString())
    }

    override fun shareRawPcmAudio(
        displayName: String,
        pcm: ShortArray,
        sampleRateHz: Int,
    ): Boolean {
        logShareDebug(
            "share raw pcm start displayName=$displayName pcmSamples=${pcm.size} sampleRateHz=$sampleRateHz",
        )
        val shareUri =
            generatedAudioTemporaryShareGateway.createShareUri(
                displayName = displayName,
                pcm = pcm,
                pcmFilePath = null,
                sampleRateHz = sampleRateHz,
                metadata = null,
            )
        if (shareUri == null) {
            logShareFailure("share raw pcm failed: createShareUri returned null displayName=$displayName")
            return false
        }
        return shareAudio(displayName, shareUri.toString())
    }

    override fun shareAudio(
        displayName: String,
        uriString: String,
    ): Boolean =
        runCatching {
            logShareDebug("share audio start displayName=$displayName uri=$uriString")
            val uri = Uri.parse(uriString)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/wav"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(displayName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val chooserIntent =
                Intent
                    .createChooser(
                        shareIntent,
                        appContext.getString(R.string.library_share_chooser_title),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(chooserIntent)
            logShareDebug("share audio chooser launched displayName=$displayName uri=$uriString")
            true
        }.getOrElse { throwable ->
            if (throwable is ActivityNotFoundException || throwable is SecurityException) {
                logShareFailure("share audio failed displayName=$displayName uri=$uriString", throwable)
                false
            } else {
                logShareFailure("share audio failed displayName=$displayName uri=$uriString", throwable)
                false
            }
        }

    private companion object {
        const val VoiceExportTag = "VoiceExportDiag"

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
