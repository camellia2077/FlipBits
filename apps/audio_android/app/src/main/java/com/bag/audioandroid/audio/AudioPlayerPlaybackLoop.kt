package com.bag.audioandroid.audio

import android.media.AudioTrack
import java.io.BufferedInputStream

internal fun runStaticPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    pcm: ShortArray,
    sampleRateHz: Int,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int = pcm.size,
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val bufferedSamples = pcm.size
    track.write(pcm, 0, pcm.size)
    val initialPosition = playbackStartOffsetSamples.coerceIn(0, reportedTotalSamples)
    onProgressChanged(initialPosition, reportedTotalSamples)
    track.play()
    val progressClock =
        PlaybackProgressClock(
            sampleRateHz = sampleRateHz,
            playbackStartOffsetSamples = playbackStartOffsetSamples,
            reportedTotalSamples = reportedTotalSamples,
            playbackLimitSamples = bufferedSamples,
        ).apply {
            reset(track.playbackHeadPosition, playback.playbackSpeed)
        }
    while (!playback.stopRequested && track.playbackHeadPosition < bufferedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed)
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        onProgressChanged(progressClock.interpolatedProgress(track.playbackHeadPosition, playback.playbackSpeed), reportedTotalSamples)
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

internal fun runStreamingPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    input: BufferedInputStream,
    sampleRateHz: Int,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int,
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val sampleBuffer = ShortArray(StreamingChunkSamples)
    var streamedSamples = 0
    onProgressChanged(playbackStartOffsetSamples, reportedTotalSamples)
    safelyPlayTrack(track)
    while (!playback.stopRequested) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
        }
        val samplesRead = readShortSamples(input, sampleBuffer)
        if (samplesRead <= 0) {
            break
        }
        var writeOffset = 0
        while (writeOffset < samplesRead && !playback.stopRequested) {
            if (playback.pauseRequested) {
                safelyPauseTrack(track)
                while (playback.pauseRequested && !playback.stopRequested) {
                    Thread.sleep(PlaybackProgressUpdateIntervalMs)
                }
                if (playback.stopRequested) {
                    return PlaybackResult.Stopped
                }
                safelyPlayTrack(track)
            }
            val written = track.write(sampleBuffer, writeOffset, samplesRead - writeOffset, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                return PlaybackResult.Stopped
            }
            writeOffset += written
            streamedSamples += written
            val reportedProgress =
                (playbackStartOffsetSamples + streamedSamples)
                    .coerceIn(0, reportedTotalSamples)
            onProgressChanged(reportedProgress, reportedTotalSamples)
        }
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    val progressClock =
        PlaybackProgressClock(
            sampleRateHz = sampleRateHz,
            playbackStartOffsetSamples = playbackStartOffsetSamples,
            reportedTotalSamples = reportedTotalSamples,
            playbackLimitSamples = streamedSamples,
        ).apply {
            reset(track.playbackHeadPosition, playback.playbackSpeed)
        }
    while (!playback.stopRequested && track.playbackHeadPosition < streamedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed)
        }
        onProgressChanged(progressClock.interpolatedProgress(track.playbackHeadPosition, playback.playbackSpeed), reportedTotalSamples)
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

private fun readShortSamples(
    input: BufferedInputStream,
    buffer: ShortArray,
): Int {
    val byteBuffer = ByteArray(buffer.size * 2)
    val bytesRead = input.read(byteBuffer)
    if (bytesRead <= 0) {
        return 0
    }
    val sampleCount = bytesRead / 2
    var byteIndex = 0
    repeat(sampleCount) { sampleIndex ->
        val low = byteBuffer[byteIndex].toInt() and 0xFF
        val high = byteBuffer[byteIndex + 1].toInt() shl 8
        buffer[sampleIndex] = (high or low).toShort()
        byteIndex += 2
    }
    return sampleCount
}

private class PlaybackProgressClock(
    private val sampleRateHz: Int,
    private val playbackStartOffsetSamples: Int,
    private val reportedTotalSamples: Int,
    private val playbackLimitSamples: Int,
) {
    private var anchorHeadSamples: Int = 0
    private var anchorTimeNanos: Long = 0L
    private var anchorPlaybackSpeed: Float = 1.0f

    fun reset(
        playbackHeadSamples: Int,
        playbackSpeed: Float,
    ) {
        anchorHeadSamples = playbackHeadSamples.coerceIn(0, playbackLimitSamples)
        anchorTimeNanos = System.nanoTime()
        anchorPlaybackSpeed = playbackSpeed.coerceAtLeast(MinInterpolatedPlaybackSpeed)
    }

    fun interpolatedProgress(
        playbackHeadSamples: Int,
        playbackSpeed: Float,
    ): Int {
        val nowNanos = System.nanoTime()
        val currentSpeed = playbackSpeed.coerceAtLeast(MinInterpolatedPlaybackSpeed)
        val estimatedHeadSamples = estimateHeadSamples(nowNanos, currentSpeed)
        val clampedPlaybackHeadSamples = playbackHeadSamples.coerceIn(0, playbackLimitSamples)
        val driftSamples = kotlin.math.abs(clampedPlaybackHeadSamples - estimatedHeadSamples)
        if (
            currentSpeed != anchorPlaybackSpeed ||
            clampedPlaybackHeadSamples < anchorHeadSamples ||
            driftSamples > InterpolationMaxDriftSamples
        ) {
            reset(clampedPlaybackHeadSamples, currentSpeed)
            return absoluteProgress(clampedPlaybackHeadSamples)
        }

        val progress = absoluteProgress(estimatedHeadSamples)
        if (nowNanos - anchorTimeNanos >= PlaybackHeadAnchorRefreshIntervalNanos) {
            reset(clampedPlaybackHeadSamples, currentSpeed)
        }
        return progress
    }

    private fun estimateHeadSamples(
        nowNanos: Long,
        playbackSpeed: Float,
    ): Int {
        val elapsedNanos = (nowNanos - anchorTimeNanos).coerceAtLeast(0L)
        val elapsedSamples =
            ((elapsedNanos.toDouble() / NanosPerSecond.toDouble()) * sampleRateHz.toDouble() * playbackSpeed.toDouble())
                .toInt()
        return (anchorHeadSamples + elapsedSamples).coerceIn(0, playbackLimitSamples)
    }

    private fun absoluteProgress(playbackHeadSamples: Int): Int =
        (playbackStartOffsetSamples + playbackHeadSamples)
            .coerceIn(0, reportedTotalSamples)
}

// Polling interval for playback progress updates (in milliseconds)
private const val PlaybackProgressUpdateIntervalMs = 16L
private const val PlaybackHeadAnchorRefreshIntervalNanos = 50_000_000L
private const val NanosPerSecond = 1_000_000_000L
private const val MinInterpolatedPlaybackSpeed = 0.1f
private const val InterpolationMaxDriftSamples = 1024
private const val StreamingChunkSamples = 4096
