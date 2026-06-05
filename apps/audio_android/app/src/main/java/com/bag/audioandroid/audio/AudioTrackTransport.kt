package com.bag.audioandroid.audio

import android.media.AudioTrack
import com.bag.audioandroid.util.safeDebugLog
import kotlin.math.roundToInt

internal fun safelyStopTrack(track: AudioTrack) {
    try {
        if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
            track.stop()
        }
    } catch (_: IllegalStateException) {
        // Ignore stop races while the track is being torn down.
    }
}

internal fun safelyFadeOutAndStopTrack(track: AudioTrack) {
    val startedAtNanos = System.nanoTime()
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "fadeOutStart playState=${track.playState} head=${track.safePlaybackHeadPosition() ?: -1} " +
            "sampleRate=${track.sampleRate} speed=${track.safePlaybackParamsSpeed() ?: "_"} " +
            "rate=${track.safePlaybackRate() ?: -1} steps=$PlaybackStopFadeSteps stepMs=$PlaybackStopFadeStepMs",
    )
    try {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            var step = PlaybackStopFadeSteps
            while (step >= 0 && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.setVolume(step.toFloat() / PlaybackStopFadeSteps.toFloat())
                if (step > 0) {
                    Thread.sleep(PlaybackStopFadeStepMs)
                }
                step -= 1
            }
        }
    } catch (_: Exception) {
        // Fall through to the hard stop path if volume fades race with teardown.
    }
    safelyStopTrack(track)
    val elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000L
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "fadeOutStop playState=${track.playState} head=${track.safePlaybackHeadPosition() ?: -1} elapsedMs=$elapsedMs",
    )
    try {
        track.setVolume(1.0f)
    } catch (_: Exception) {
        // Ignore volume reset races while the track is being torn down.
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
    sampleIndex: Int,
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

internal fun setPlaybackSpeedSafely(
    track: AudioTrack,
    playbackSpeed: Float,
): Boolean {
    val resolvedPlaybackSpeed = playbackSpeed.coerceAtLeast(PlaybackSpeedMin)
    val basePlaybackRate = track.sampleRate.coerceAtLeast(1)
    val beforeSpeed = track.safePlaybackParamsSpeed()
    val beforeRate = track.safePlaybackRate()
    val rateReset = setPlaybackRateSafely(track, basePlaybackRate)
    return try {
        track.playbackParams =
            track.playbackParams
                .allowDefaults()
                .setPitch(1.0f)
                .setSpeed(resolvedPlaybackSpeed)
        safeDebugLog(
            AudioTrackTransportDiagTag,
            "setPlaybackParams requested=$playbackSpeed resolved=$resolvedPlaybackSpeed " +
                "beforeSpeed=${beforeSpeed ?: "_"} actualSpeed=${track.safePlaybackParamsSpeed() ?: "_"} " +
                "beforeRate=${beforeRate ?: -1} rateReset=$rateReset actualRate=${track.safePlaybackRate() ?: -1} " +
                "playState=${track.playState}",
        )
        true
    } catch (error: Exception) {
        val paramsReset = setPlaybackParamsSpeedSafely(track, 1.0f)
        val fallbackApplied = setPlaybackRateFallback(track, resolvedPlaybackSpeed)
        safeDebugLog(
            AudioTrackTransportDiagTag,
            "setPlaybackParamsFailed requested=$playbackSpeed resolved=$resolvedPlaybackSpeed " +
                "beforeSpeed=${beforeSpeed ?: "_"} rateReset=$rateReset paramsReset=$paramsReset " +
                "fallbackApplied=$fallbackApplied actualSpeed=${track.safePlaybackParamsSpeed() ?: "_"} " +
                "actualRate=${track.safePlaybackRate() ?: -1} error=${error::class.java.simpleName}",
        )
        fallbackApplied
    }
}

@Suppress("DEPRECATION")
private fun setPlaybackRateFallback(
    track: AudioTrack,
    playbackSpeed: Float,
): Boolean =
    setPlaybackRateSafely(
        track = track,
        playbackRate = (track.sampleRate * playbackSpeed).roundToInt().coerceAtLeast(1),
    )

@Suppress("DEPRECATION")
private fun setPlaybackRateSafely(
    track: AudioTrack,
    playbackRate: Int,
): Boolean =
    try {
        track.playbackRate = playbackRate
        true
    } catch (_: Exception) {
        false
    }

private fun setPlaybackParamsSpeedSafely(
    track: AudioTrack,
    playbackSpeed: Float,
): Boolean =
    try {
        track.playbackParams =
            track.playbackParams
                .allowDefaults()
                .setPitch(1.0f)
                .setSpeed(playbackSpeed)
        true
    } catch (_: Exception) {
        false
    }

private const val AudioTrackTransportDiagTag = "AudioTrackTransport"
private const val PlaybackEdgeFadeDiagTag = "PlaybackEdgeFade"
private const val PlaybackStopFadeSteps = 6
private const val PlaybackStopFadeStepMs = 2L

private fun AudioTrack.safePlaybackParamsSpeed(): Float? =
    try {
        playbackParams.speed
    } catch (_: Exception) {
        null
    }

private fun AudioTrack.safePlaybackRate(): Int? =
    try {
        playbackRate
    } catch (_: Exception) {
        null
    }

internal fun AudioTrack.safePlaybackHeadPosition(): Int? =
    try {
        playbackHeadPosition
    } catch (_: Exception) {
        null
    }
