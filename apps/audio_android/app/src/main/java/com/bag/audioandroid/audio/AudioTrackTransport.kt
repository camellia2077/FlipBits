package com.bag.audioandroid.audio

import android.media.AudioTrack

internal fun safelyStopTrack(track: AudioTrack) {
    try {
        if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
            track.stop()
        }
    } catch (_: IllegalStateException) {
        // Ignore stop races while the track is being torn down.
    }
}

internal fun safelyPauseTrack(track: AudioTrack) {
    try {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.pause()
        }
    } catch (_: IllegalStateException) {
        // Ignore pause races while the track is being torn down.
    }
}

internal fun safelyPlayTrack(track: AudioTrack) {
    try {
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }
    } catch (_: IllegalStateException) {
        // Ignore play races while the track is being torn down.
    }
}

internal fun setPlaybackHeadPositionSafely(
    track: AudioTrack,
    sampleIndex: Int
): Boolean =
    try {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.pause()
        }
        track.setPlaybackHeadPosition(sampleIndex)
        true
    } catch (_: IllegalStateException) {
        false
    }
