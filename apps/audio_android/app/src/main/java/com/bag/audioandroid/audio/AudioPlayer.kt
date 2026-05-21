package com.bag.audioandroid.audio

import android.media.AudioTrack
import com.bag.audioandroid.util.safeDebugLog
import java.io.BufferedInputStream
import java.io.FileInputStream

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

        @Volatile
        var playbackSpeed = 1.0f

        @Volatile
        var seekVersion = 0
    }

    fun prepareForNewPlayback(): PlaybackHandle = PlaybackHandle().also { currentPlayback = it }

    fun playPcm(
        playback: PlaybackHandle,
        pcm: ShortArray,
        sampleRateHz: Int,
        playbackSpeed: Float = 1.0f,
        startSampleIndex: Int = 0,
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
    ): PlaybackResult {
        val totalSamples = pcm.size
        val startOffsetSamples = startSampleIndex.coerceIn(0, totalSamples)
        safeDebugLog(
            AudioPlayerDiagTag,
            "playPcm handle=${playback.debugHandleId()} total=$totalSamples sampleRate=$sampleRateHz " +
                "speed=$playbackSpeed start=$startOffsetSamples",
        )
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
        val resolvedPlaybackSpeed = playbackSpeed.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)

        currentPlayback = playback
        playback.track = track
        playback.playbackSpeed = resolvedPlaybackSpeed
        playback.totalSamples = totalSamples
        playback.bufferStartSamples = startOffsetSamples
        playback.bufferedSamples = playbackPcm.size
        return try {
            setPlaybackSpeedSafely(track, resolvedPlaybackSpeed)
            runStaticPlaybackLoop(
                playback = playback,
                track = track,
                pcm = playbackPcm,
                sampleRateHz = sampleRateHz,
                playbackStartOffsetSamples = startOffsetSamples,
                reportedTotalSamples = totalSamples,
                onProgressChanged = onProgressChanged,
            )
        } finally {
            releasePlaybackTrack(playback, track)
        }
    }

    fun playPcmFile(
        playback: PlaybackHandle,
        pcmFilePath: String,
        sampleRateHz: Int,
        totalSamples: Int,
        playbackSpeed: Float = 1.0f,
        startSampleIndex: Int = 0,
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
    ): PlaybackResult {
        val startOffsetSamples = startSampleIndex.coerceIn(0, totalSamples)
        safeDebugLog(
            AudioPlayerDiagTag,
            "playPcmFile handle=${playback.debugHandleId()} total=$totalSamples sampleRate=$sampleRateHz " +
                "speed=$playbackSpeed start=$startOffsetSamples path=$pcmFilePath",
        )
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
        val track = createStreamingAudioTrack(sampleRateHz)
        val resolvedPlaybackSpeed = playbackSpeed.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)

        currentPlayback = playback
        playback.track = track
        playback.playbackSpeed = resolvedPlaybackSpeed
        playback.totalSamples = totalSamples
        playback.bufferStartSamples = startOffsetSamples
        playback.bufferedSamples = totalSamples - startOffsetSamples
        return try {
            setPlaybackSpeedSafely(track, resolvedPlaybackSpeed)
            BufferedInputStream(FileInputStream(pcmFilePath), StreamingBufferBytes).use { input ->
                skipFully(input, startOffsetSamples.toLong() * ShortBytes.toLong())
                runStreamingPlaybackLoop(
                    playback = playback,
                    track = track,
                    input = input,
                    sampleRateHz = sampleRateHz,
                    playbackStartOffsetSamples = startOffsetSamples,
                    reportedTotalSamples = totalSamples,
                    onProgressChanged = onProgressChanged,
                )
            }
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
            playback.seekVersion += 1
            clampedPosition
        } else {
            null
        }
    }

    fun pause() {
        currentPlayback?.let { playback ->
            safeDebugLog(
                AudioPlayerDiagTag,
                "pause handle=${playback.debugHandleId()} track=${playback.track != null}",
            )
            playback.pauseRequested = true
            playback.track?.let(::safelyPauseTrack)
        }
    }

    fun resume() {
        currentPlayback?.let { playback ->
            safeDebugLog(
                AudioPlayerDiagTag,
                "resume handle=${playback.debugHandleId()} track=${playback.track != null} speed=${playback.playbackSpeed}",
            )
            playback.pauseRequested = false
            playback.track?.let { track ->
                setPlaybackSpeedSafely(track, playback.playbackSpeed)
                safelyPlayTrack(track)
            }
        }
    }

    fun setPlaybackSpeed(playbackSpeed: Float): Boolean {
        val playback = currentPlayback ?: return false
        val resolvedPlaybackSpeed = playbackSpeed.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)
        safeDebugLog(
            AudioPlayerDiagTag,
            "setSpeed handle=${playback.debugHandleId()} from=${playback.playbackSpeed} to=$resolvedPlaybackSpeed",
        )
        playback.playbackSpeed = resolvedPlaybackSpeed
        val track = playback.track ?: return true
        return setPlaybackSpeedSafely(track, resolvedPlaybackSpeed)
    }

    fun stop() {
        currentPlayback?.let { playback ->
            safeDebugLog(
                AudioPlayerDiagTag,
                "stop handle=${playback.debugHandleId()} track=${playback.track != null} " +
                    "stopRequested=${playback.stopRequested} pauseRequested=${playback.pauseRequested}",
            )
            playback.stopRequested = true
            playback.pauseRequested = false
            playback.track?.let(::safelyStopTrack)
        }
    }

    private fun releasePlaybackTrack(
        playback: PlaybackHandle,
        track: AudioTrack,
    ) {
        safeDebugLog(
            AudioPlayerDiagTag,
            "releaseTrack handle=${playback.debugHandleId()} playState=${track.playState} " +
                "bufferStart=${playback.bufferStartSamples} buffered=${playback.bufferedSamples}",
        )
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

    private companion object {
        const val MinPlaybackSpeed = 0.1f
        const val MaxPlaybackSpeed = 4.0f
        const val ShortBytes = 2
        const val StreamingBufferBytes = 32 * 1024
    }
}

private const val AudioPlayerDiagTag = "AudioPlayerDiag"

private fun AudioPlayer.PlaybackHandle.debugHandleId(): String = Integer.toHexString(System.identityHashCode(this))

private fun skipFully(
    input: BufferedInputStream,
    bytesToSkip: Long,
) {
    var remaining = bytesToSkip
    while (remaining > 0) {
        val skipped = input.skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
            continue
        }
        if (input.read() == -1) {
            break
        }
        remaining -= 1
    }
}
