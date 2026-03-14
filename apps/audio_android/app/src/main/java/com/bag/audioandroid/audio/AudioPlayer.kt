package com.bag.audioandroid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

enum class PlaybackResult {
    Completed,
    Stopped
}

@Suppress("MagicNumber")
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
    }

    fun prepareForNewPlayback(): PlaybackHandle =
        PlaybackHandle().also { currentPlayback = it }

    fun playPcm(
        playback: PlaybackHandle,
        pcm: ShortArray,
        sampleRateHz: Int,
        startSampleIndex: Int = 0,
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> }
    ): PlaybackResult {
        val totalSamples = pcm.size
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRateHz)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        currentPlayback = playback
        playback.track = track
        playback.totalSamples = totalSamples
        try {
            track.write(pcm, 0, pcm.size)
            val initialPosition = startSampleIndex.coerceIn(0, totalSamples)
            if (initialPosition > 0) {
                setPlaybackHeadPosition(track, initialPosition)
            }
            onProgressChanged(initialPosition, totalSamples)
            track.play()
            while (!playback.stopRequested && track.playbackHeadPosition < totalSamples) {
                if (playback.pauseRequested) {
                    safelyPause(track)
                    while (playback.pauseRequested && !playback.stopRequested) {
                        Thread.sleep(PROGRESS_UPDATE_INTERVAL_MS)
                    }
                    if (playback.stopRequested) {
                        return PlaybackResult.Stopped
                    }
                    safelyPlay(track)
                }
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    Thread.sleep(PROGRESS_UPDATE_INTERVAL_MS)
                    continue
                }
                onProgressChanged(track.playbackHeadPosition.coerceIn(0, totalSamples), totalSamples)
                Thread.sleep(PROGRESS_UPDATE_INTERVAL_MS)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            onProgressChanged(totalSamples, totalSamples)
            return PlaybackResult.Completed
        } finally {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                safelyStop(track)
            }
            if (playback.track === track) {
                playback.track = null
            }
            if (currentPlayback === playback) {
                currentPlayback = null
            }
            playback.stopRequested = false
            playback.pauseRequested = false
            track.release()
        }
    }

    fun seekTo(sampleIndex: Int): Int? {
        val playback = currentPlayback ?: return null
        val track = playback.track ?: return null
        val clampedPosition = sampleIndex.coerceIn(0, playback.totalSamples)
        if (!setPlaybackHeadPosition(track, clampedPosition)) {
            return null
        }
        return clampedPosition
    }

    fun pause() {
        currentPlayback?.let { playback ->
            playback.pauseRequested = true
            playback.track?.let(::safelyPause)
        }
    }

    fun resume() {
        currentPlayback?.let { playback ->
            playback.pauseRequested = false
            playback.track?.let(::safelyPlay)
        }
    }

    fun stop() {
        currentPlayback?.let { playback ->
            playback.stopRequested = true
            playback.pauseRequested = false
            playback.track?.let(::safelyStop)
        }
    }

    private fun safelyStop(track: AudioTrack) {
        try {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop()
            }
        } catch (_: IllegalStateException) {
            // Ignore stop races while the track is being torn down.
        }
    }

    private fun safelyPause(track: AudioTrack) {
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
        } catch (_: IllegalStateException) {
            // Ignore pause races while the track is being torn down.
        }
    }

    private fun safelyPlay(track: AudioTrack) {
        try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
        } catch (_: IllegalStateException) {
            // Ignore play races while the track is being torn down.
        }
    }

    private fun setPlaybackHeadPosition(track: AudioTrack, sampleIndex: Int): Boolean =
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
            track.setPlaybackHeadPosition(sampleIndex)
            true
        } catch (_: IllegalStateException) {
            false
        }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 50L
    }
}
