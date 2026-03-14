package com.bag.audioandroid.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioShareGateway
import com.bag.audioandroid.domain.SavedAudioItem

class AndroidIntentAudioShareGateway(
    private val appContext: Context
) : AudioShareGateway {
    override fun shareSavedAudio(item: SavedAudioItem): Boolean =
        runCatching {
            val uri = Uri.parse(item.uriString)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(item.displayName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(
                shareIntent,
                appContext.getString(R.string.library_share_chooser_title)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(chooserIntent)
            true
        }.getOrElse { throwable ->
            if (throwable is ActivityNotFoundException || throwable is SecurityException) {
                false
            } else {
                false
            }
        }
}
