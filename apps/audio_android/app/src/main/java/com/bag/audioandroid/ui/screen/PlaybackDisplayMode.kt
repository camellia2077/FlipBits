package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.R

enum class PlaybackDisplayMode(
    val titleResId: Int,
) {
    Visual(R.string.audio_playback_view_visual),
    Mix(R.string.audio_playback_view_mix),
    Lyrics(R.string.audio_playback_view_lyrics),
}

data class DebugPlaybackDisplayModeRequest(
    val requestId: Long,
    val mode: PlaybackDisplayMode,
)

data class DebugMorseVisualizationModeRequest(
    val requestId: Long,
    val mode: MiniMorseVisualizationMode,
)

fun String?.toPlaybackDisplayMode(): PlaybackDisplayMode =
    when (this?.trim()?.lowercase()) {
        "visual" -> PlaybackDisplayMode.Visual
        "mix" -> PlaybackDisplayMode.Mix
        else -> PlaybackDisplayMode.Lyrics
    }
