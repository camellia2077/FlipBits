package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.R

internal enum class PlaybackFollowViewMode(
    val titleResId: Int,
) {
    Hex(R.string.audio_follow_view_hex),
    Binary(R.string.audio_follow_view_binary),
    Morse(R.string.audio_follow_view_morse),
}
