package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.R

internal enum class PlaybackDisplayMode(
    val titleResId: Int,
) {
    Visual(R.string.audio_playback_view_visual),
    Lyrics(R.string.audio_playback_view_lyrics),
}
