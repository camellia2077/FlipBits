package com.bag.audioandroid.audio

internal const val PlaybackSpeedNormal = 1.0f
internal const val PlaybackSpeedMin = 0.1f
internal const val PlaybackSpeedMax = 4.0f

private const val PlaybackSpeedRenderEpsilon = 0.001f

internal fun coercePlaybackSpeed(playbackSpeed: Float): Float = playbackSpeed.coerceIn(PlaybackSpeedMin, PlaybackSpeedMax)

internal fun shouldRenderSpeedAdjustedPcm(playbackSpeed: Float): Boolean =
    playbackSpeed < PlaybackSpeedNormal - PlaybackSpeedRenderEpsilon ||
        playbackSpeed > PlaybackSpeedNormal + PlaybackSpeedRenderEpsilon
