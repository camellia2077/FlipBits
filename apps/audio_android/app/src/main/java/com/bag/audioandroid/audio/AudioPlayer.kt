package com.bag.audioandroid.audio

import android.media.AudioTrack

enum class PlaybackResult {
    Completed,
    Stopped,
}

class AudioPlayer {
    @Volatile
    private var currentPlayback: PlaybackHandle? = null

    class PlaybackHandle internal constructor() {
        @Volatile
        var track: AudioTrack? = null

        @Volatile
        var stopRequested = false

        @Volatile
        var pauseRequested = false

        @Volatile
        var totalSamples = 0

        @Volatile
        var bufferStartSamples = 0

        @Volatile
        var bufferedSamples = 0
    }

    fun prepareForNewPlayback(): PlaybackHandle = PlaybackHandle().also { currentPlayback = it }

    fun playPcm(
        playback: PlaybackHandle,
        pcm: ShortArray,
        sampleRateHz: Int,
        startSampleIndex: Int = 0,
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
    ): PlaybackResult {
        val totalSamples = pcm.size
        val startOffsetSamples = startSampleIndex.coerceIn(0, totalSamples)
        if (startOffsetSamples >= totalSamples) {
            currentPlayback = playback
            playback.track = null
            playback.totalSamples = totalSamples
            playback.bufferStartSamples = totalSamples
            playback.bufferedSamples = 0
            onProgressChanged(totalSamples, totalSamples)
            if (currentPlayback === playback) {
                currentPlayback = null
            }
            playback.stopRequested = false
            playback.pauseRequested = false
            return PlaybackResult.Completed
        }
        val playbackPcm =
            if (startOffsetSamples > 0) {
                pcm.copyOfRange(startOffsetSamples, totalSamples)
            } else {
                pcm
            }
        val track =
            createStaticAudioTrack(
                sampleRateHz = sampleRateHz,
                sampleCount = playbackPcm.size,
            )

        currentPlayback = playback
        playback.track = track
        playback.totalSamples = totalSamples
        playback.bufferStartSamples = startOffsetSamples
        playback.bufferedSamples = playbackPcm.size
        return try {
            runStaticPlaybackLoop(
                playback = playback,
                track = track,
                pcm = playbackPcm,
                playbackStartOffsetSamples = startOffsetSamples,
                reportedTotalSamples = totalSamples,
                onProgressChanged = onProgressChanged,
            )
        } finally {
            releasePlaybackTrack(playback, track)
        }
    }

    fun seekTo(sampleIndex: Int): Int? {
        val playback = currentPlayback ?: return null
        val track = playback.track ?: return null
        val clampedPosition = sampleIndex.coerceIn(0, playback.totalSamples)
        if (clampedPosition < playback.bufferStartSamples ||
            clampedPosition > playback.bufferStartSamples + playback.bufferedSamples
        ) {
            return null
        }
        val relativePosition = clampedPosition - playback.bufferStartSamples
        return if (setPlaybackHeadPositionSafely(track, relativePosition)) {
            clampedPosition
        } else {
            null
        }
    }

    fun pause() {
        currentPlayback?.let { playback ->
            playback.pauseRequested = true
            playback.track?.let(::safelyPauseTrack)
        }
    }

    fun resume() {
        currentPlayback?.let { playback ->
            playback.pauseRequested = false
            playback.track?.let(::safelyPlayTrack)
        }
    }

    fun stop() {
        currentPlayback?.let { playback ->
            playback.stopRequested = true
            playback.pauseRequested = false
            playback.track?.let(::safelyStopTrack)
        }
    }

    private fun releasePlaybackTrack(
        playback: PlaybackHandle,
        track: AudioTrack,
    ) {
        if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
            safelyStopTrack(track)
        }
        if (playback.track === track) {
            playback.track = null
        }
        if (currentPlayback === playback) {
            currentPlayback = null
        }
        playback.stopRequested = false
        playback.pauseRequested = false
        playback.bufferStartSamples = 0
        playback.bufferedSamples = 0
        track.release()
    }
}
