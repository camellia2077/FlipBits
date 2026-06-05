package com.bag.audioandroid.audio

import android.media.AudioTrack
import com.bag.audioandroid.util.safeDebugLog
import java.io.BufferedInputStream

internal fun runStaticPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    pcm: ShortArray,
    sampleRateHz: Int,
    playbackStartOffsetSamples: Int = 0,
    reportedTotalSamples: Int = pcm.size,
    onPlaybackStarted: () -> Unit = {},
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val bufferedSamples = pcm.size
    logPlaybackFadeIn(
        path = "static",
        absoluteStartSamples = 0,
        sampleCount = bufferedSamples,
        sampleRateHz = sampleRateHz,
    )
    val playbackPcm = playbackStartFadedChunk(pcm, 0, pcm.size, sampleRateHz)
    track.write(playbackPcm, 0, playbackPcm.size)
    val initialPosition = playbackStartOffsetSamples.coerceIn(0, reportedTotalSamples)
    onProgressChanged(initialPosition, reportedTotalSamples)
    track.play()
    onPlaybackStarted()
    val progressClock =
        PlaybackProgressClock(
            sampleRateHz = sampleRateHz,
            playbackStartOffsetSamples = playbackStartOffsetSamples,
            reportedTotalSamples = reportedTotalSamples,
            playbackLimitSamples = bufferedSamples,
        ).apply {
            reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
        }
    while (!playback.stopRequested && track.playbackHeadPosition < bufferedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        onProgressChanged(
            progressClock.interpolatedProgress(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion),
            reportedTotalSamples,
        )
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
    onPlaybackStarted: () -> Unit = {},
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val sampleBuffer = ShortArray(StreamingChunkSamples)
    var streamedSamples = 0
    var playbackStarted = false
    onProgressChanged(playbackStartOffsetSamples, reportedTotalSamples)
    while (!playback.stopRequested) {
        if (playback.pauseRequested) {
            if (playbackStarted) {
                safelyPauseTrack(track)
            }
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            if (playbackStarted) {
                safelyPlayTrack(track)
            }
        }
        val samplesRead = readShortSamples(input, sampleBuffer)
        if (samplesRead <= 0) {
            break
        }
        logPlaybackFadeIn(
            path = "file-stream",
            absoluteStartSamples = streamedSamples,
            sampleCount = samplesRead,
            sampleRateHz = sampleRateHz,
        )
        applyPlaybackStartFadeInPlace(
            samples = sampleBuffer,
            absoluteStartSamples = streamedSamples,
            sampleRateHz = sampleRateHz,
            sampleCount = samplesRead,
        )
        var writeOffset = 0
        while (writeOffset < samplesRead && !playback.stopRequested) {
            if (playback.pauseRequested) {
                if (playbackStarted) {
                    safelyPauseTrack(track)
                }
                while (playback.pauseRequested && !playback.stopRequested) {
                    Thread.sleep(PlaybackProgressUpdateIntervalMs)
                }
                if (playback.stopRequested) {
                    return PlaybackResult.Stopped
                }
                if (playbackStarted) {
                    safelyPlayTrack(track)
                }
            }
            val written =
                track.write(
                    sampleBuffer,
                    writeOffset,
                    samplesRead - writeOffset,
                    playbackStreamWriteMode(playbackStarted),
                )
            if (written <= 0) {
                if (!playbackStarted) {
                    playbackStarted = startPrimedPlayback(track, "file-stream", writeOffset, onPlaybackStarted)
                    continue
                }
                return PlaybackResult.Stopped
            }
            if (!playbackStarted) {
                playbackStarted = startPrimedPlayback(track, "file-stream", writeOffset + written, onPlaybackStarted)
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
            reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
        }
    while (!playback.stopRequested && track.playbackHeadPosition < streamedSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion)
        }
        onProgressChanged(
            progressClock.interpolatedProgress(track.playbackHeadPosition, playback.playbackSpeed, playback.seekVersion),
            reportedTotalSamples,
        )
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(reportedTotalSamples, reportedTotalSamples)
    return PlaybackResult.Completed
}

internal fun runRenderedPcmStreamingPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    pcm: ShortArray,
    sampleRateHz: Int,
    onPlaybackStarted: () -> Unit = {},
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    val totalSamples = pcm.size
    var playbackStarted = false
    onProgressChanged(0, totalSamples)
    val progressClock =
        PlaybackProgressClock(
            sampleRateHz = sampleRateHz,
            playbackStartOffsetSamples = 0,
            reportedTotalSamples = totalSamples,
            playbackLimitSamples = totalSamples,
        ).apply {
            reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
        }
    var writeOffset = 0
    while (!playback.stopRequested && writeOffset < totalSamples) {
        if (playback.pauseRequested) {
            if (playbackStarted) {
                safelyPauseTrack(track)
            }
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            if (playbackStarted) {
                safelyPlayTrack(track)
                progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            }
        }
        val samplesToWrite = minOf(StreamingChunkSamples, totalSamples - writeOffset)
        logPlaybackFadeIn(
            path = "rendered-stream",
            absoluteStartSamples = writeOffset,
            sampleCount = samplesToWrite,
            sampleRateHz = sampleRateHz,
        )
        val chunk =
            playbackStartFadedChunk(
                source = pcm,
                sourceOffset = writeOffset,
                sampleCount = samplesToWrite,
                sampleRateHz = sampleRateHz,
            )
        val written = track.write(chunk, 0, chunk.size, playbackStreamWriteMode(playbackStarted))
        if (written <= 0) {
            if (!playbackStarted) {
                playbackStarted = startPrimedPlayback(track, "rendered-stream", 0, onPlaybackStarted)
                progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
                continue
            }
            return PlaybackResult.Stopped
        }
        if (!playbackStarted) {
            playbackStarted = startPrimedPlayback(track, "rendered-stream", written, onPlaybackStarted)
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
        }
        writeOffset += written
        onProgressChanged(
            progressClock.interpolatedProgress(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion),
            totalSamples,
        )
    }
    while (!playback.stopRequested && track.playbackHeadPosition < totalSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        onProgressChanged(
            progressClock.interpolatedProgress(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion),
            totalSamples,
        )
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(totalSamples, totalSamples)
    return PlaybackResult.Completed
}

internal fun runSpeedAdjustedPcmRenderPlaybackLoop(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
    sourcePcm: ShortArray,
    playbackSpeed: Float,
    sampleRateHz: Int,
    renderedTotalSamples: Int,
    renderChunk: (
        outputStartSamples: Int,
        outputSampleCount: Int,
    ) -> ShortArray = { outputStartSamples, outputSampleCount ->
        renderSpeedAdjustedPcmChunk(
            sourcePcm = sourcePcm,
            outputStartSamples = outputStartSamples,
            outputSampleCount = outputSampleCount,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
        )
    },
    onPlaybackStarted: () -> Unit = {},
    onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
): PlaybackResult {
    var playbackStarted = false
    onProgressChanged(0, renderedTotalSamples)
    val progressClock =
        PlaybackProgressClock(
            sampleRateHz = sampleRateHz,
            playbackStartOffsetSamples = 0,
            reportedTotalSamples = renderedTotalSamples,
            playbackLimitSamples = renderedTotalSamples,
        ).apply {
            reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
        }
    var writeOffset = 0
    while (!playback.stopRequested && writeOffset < renderedTotalSamples) {
        if (playback.pauseRequested) {
            if (playbackStarted) {
                safelyPauseTrack(track)
            }
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            if (playbackStarted) {
                safelyPlayTrack(track)
                progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            }
        }
        val samplesToRender = minOf(SpeedAdjustedRenderChunkSamples, renderedTotalSamples - writeOffset)
        val chunk = renderChunk(writeOffset, samplesToRender)
        logPlaybackFadeIn(
            path = "speed-render-stream",
            absoluteStartSamples = writeOffset,
            sampleCount = chunk.size,
            sampleRateHz = sampleRateHz,
        )
        applyPlaybackStartFadeInPlace(
            samples = chunk,
            absoluteStartSamples = writeOffset,
            sampleRateHz = sampleRateHz,
        )
        var chunkOffset = 0
        while (chunkOffset < chunk.size && !playback.stopRequested) {
            val written =
                track.write(
                    chunk,
                    chunkOffset,
                    chunk.size - chunkOffset,
                    playbackStreamWriteMode(playbackStarted),
                )
            if (written <= 0) {
                if (!playbackStarted) {
                    playbackStarted = startPrimedPlayback(track, "speed-render-stream", chunkOffset, onPlaybackStarted)
                    progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
                    continue
                }
                return PlaybackResult.Stopped
            }
            if (!playbackStarted) {
                playbackStarted = startPrimedPlayback(track, "speed-render-stream", chunkOffset + written, onPlaybackStarted)
                progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            }
            chunkOffset += written
            writeOffset += written
            onProgressChanged(
                progressClock.interpolatedProgress(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion),
                renderedTotalSamples,
            )
        }
    }
    while (!playback.stopRequested && track.playbackHeadPosition < renderedTotalSamples) {
        if (playback.pauseRequested) {
            safelyPauseTrack(track)
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            while (playback.pauseRequested && !playback.stopRequested) {
                Thread.sleep(PlaybackProgressUpdateIntervalMs)
            }
            if (playback.stopRequested) {
                return PlaybackResult.Stopped
            }
            safelyPlayTrack(track)
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
        }
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            progressClock.reset(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion)
            Thread.sleep(PlaybackProgressUpdateIntervalMs)
            continue
        }
        onProgressChanged(
            progressClock.interpolatedProgress(track.playbackHeadPosition, PlaybackSpeedNormal, playback.seekVersion),
            renderedTotalSamples,
        )
        Thread.sleep(PlaybackProgressUpdateIntervalMs)
    }
    if (playback.stopRequested) {
        return PlaybackResult.Stopped
    }
    onProgressChanged(renderedTotalSamples, renderedTotalSamples)
    return PlaybackResult.Completed
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
    private var lastSeekVersion: Int = 0
    private var lastReportedHeadSamples: Int = 0

    fun reset(
        playbackHeadSamples: Int,
        playbackSpeed: Float,
        seekVersion: Int = lastSeekVersion,
    ) {
        val clampedHeadSamples = playbackHeadSamples.coerceIn(0, playbackLimitSamples)
        anchorHeadSamples =
            if (seekVersion != lastSeekVersion) {
                clampedHeadSamples
            } else {
                clampedHeadSamples.coerceAtLeast(lastReportedHeadSamples)
            }
        anchorTimeNanos = System.nanoTime()
        anchorPlaybackSpeed = playbackSpeed.coerceAtLeast(MinInterpolatedPlaybackSpeed)
        lastSeekVersion = seekVersion
        lastReportedHeadSamples = anchorHeadSamples
    }

    fun interpolatedProgress(
        playbackHeadSamples: Int,
        playbackSpeed: Float,
        seekVersion: Int,
    ): Int {
        val nowNanos = System.nanoTime()
        val currentSpeed = playbackSpeed.coerceAtLeast(MinInterpolatedPlaybackSpeed)
        val estimatedHeadSamples = estimateHeadSamples(nowNanos, currentSpeed)
        val clampedPlaybackHeadSamples = playbackHeadSamples.coerceIn(0, playbackLimitSamples)
        val seekChanged = seekVersion != lastSeekVersion
        val driftSamples = kotlin.math.abs(clampedPlaybackHeadSamples - estimatedHeadSamples)
        if (
            currentSpeed != anchorPlaybackSpeed ||
            seekChanged ||
            clampedPlaybackHeadSamples < anchorHeadSamples ||
            driftSamples > InterpolationMaxDriftSamples
        ) {
            // AudioTrack can briefly report an older playback head during startup
            // on release builds. Keep automatic playback monotonic, but still let
            // real user seeks move the reported position backward.
            if (clampedPlaybackHeadSamples < lastReportedHeadSamples && !seekChanged) {
                return absoluteProgress(lastReportedHeadSamples)
            }
            reset(clampedPlaybackHeadSamples, currentSpeed, seekVersion)
            return absoluteProgress(recordReportedHead(clampedPlaybackHeadSamples))
        }

        val progress = absoluteProgress(recordReportedHead(estimatedHeadSamples))
        if (nowNanos - anchorTimeNanos >= PlaybackHeadAnchorRefreshIntervalNanos) {
            reset(clampedPlaybackHeadSamples.coerceAtLeast(lastReportedHeadSamples), currentSpeed, seekVersion)
        }
        return progress
    }

    private fun recordReportedHead(playbackHeadSamples: Int): Int {
        lastReportedHeadSamples = playbackHeadSamples.coerceAtLeast(lastReportedHeadSamples)
        return lastReportedHeadSamples
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
private const val MinInterpolatedPlaybackSpeed = PlaybackSpeedMin
private const val InterpolationMaxDriftSamples = 1024
private const val StreamingChunkSamples = 4096
private const val SpeedAdjustedRenderChunkSamples = 16_384

private const val PlaybackEdgeFadeDiagTag = "PlaybackEdgeFade"

private fun playbackStreamWriteMode(playbackStarted: Boolean): Int =
    if (playbackStarted) {
        AudioTrack.WRITE_BLOCKING
    } else {
        AudioTrack.WRITE_NON_BLOCKING
    }

private fun startPrimedPlayback(
    track: AudioTrack,
    path: String,
    primedSamples: Int,
    onPlaybackStarted: () -> Unit,
): Boolean {
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "startAfterPrime path=$path primed=$primedSamples head=${track.safePlaybackHeadPosition() ?: -1} playState=${track.playState}",
    )
    safelyPlayTrack(track)
    onPlaybackStarted()
    return true
}

private fun logPlaybackFadeIn(
    path: String,
    absoluteStartSamples: Int,
    sampleCount: Int,
    sampleRateHz: Int,
) {
    val fadeSamples = playbackEdgeFadeSampleCount(sampleRateHz)
    val coveredSamples =
        (fadeSamples - absoluteStartSamples)
            .coerceIn(0, sampleCount.coerceAtLeast(0))
    if (coveredSamples <= 0) {
        return
    }
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "fadeIn path=$path offset=$absoluteStartSamples samples=$sampleCount " +
            "covered=$coveredSamples fadeSamples=$fadeSamples sampleRate=$sampleRateHz",
    )
}
