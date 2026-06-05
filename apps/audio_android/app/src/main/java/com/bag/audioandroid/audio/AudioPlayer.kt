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
        var playbackSpeed = PlaybackSpeedNormal

        @Volatile
        var seekVersion = 0

        @Volatile
        internal var renderedTimeline: RenderedPlaybackTimeline? = null

        @Volatile
        internal var stopFadeInProgress = false
    }

    fun prepareForNewPlayback(): PlaybackHandle = PlaybackHandle().also { currentPlayback = it }

    fun playPcm(
        playback: PlaybackHandle,
        pcm: ShortArray,
        sampleRateHz: Int,
        playbackSpeed: Float = PlaybackSpeedNormal,
        startSampleIndex: Int = 0,
        renderContext: PlaybackRenderContext = PlaybackRenderContext.Empty,
        preferStreamingSpeedAdjustedPcm: Boolean = false,
        onPlaybackStarted: () -> Unit = {},
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
            return completePlaybackAtEnd(playback, totalSamples, onProgressChanged)
        }
        val playbackPcm =
            if (startOffsetSamples > 0) {
                pcm.copyOfRange(startOffsetSamples, totalSamples)
            } else {
                pcm
            }
        val resolvedPlaybackSpeed = coercePlaybackSpeed(playbackSpeed)
        if (shouldRenderSpeedAdjustedPcm(resolvedPlaybackSpeed)) {
            if (shouldStreamSpeedAdjustedPlayback(playbackPcm.size, resolvedPlaybackSpeed, preferStreamingSpeedAdjustedPcm)) {
                return playStreamingSpeedAdjustedPcm(
                    playback = playback,
                    sourcePcm = playbackPcm,
                    sampleRateHz = sampleRateHz,
                    playbackSpeed = resolvedPlaybackSpeed,
                    sourceStartSamples = startOffsetSamples,
                    sourceTotalSamples = totalSamples,
                    renderContext = renderContext,
                    onPlaybackStarted = onPlaybackStarted,
                    onProgressChanged = onProgressChanged,
                )
            }
            val renderTrace =
                PlaybackSpeedMemoryRenderTrace(
                    handleId = playback.debugHandleId(),
                    playbackMode = renderContext.diagPlaybackMode(),
                    playbackSpeed = resolvedPlaybackSpeed,
                    streaming = false,
                    fileBacked = false,
                    sourceSampleCount = playbackPcm.size,
                )
            val renderPlan =
                renderTrace.measureRender {
                    buildPreRenderedSpeedAdjustedRenderPlan(
                        sourcePcm = playbackPcm,
                        sourceStartSamples = startOffsetSamples,
                        sourceTotalSamples = totalSamples,
                        playbackSpeed = resolvedPlaybackSpeed,
                        sampleRateHz = sampleRateHz,
                        context = renderContext,
                    )
                }
            renderTrace.log(
                event = "preRendered",
                rendererType = renderPlan.rendererType,
                renderedSampleCount = renderPlan.renderedPlayback.pcm.size,
            )
            return playRenderedSpeedAdjustedPcm(
                playback = playback,
                renderedPlayback = renderPlan.renderedPlayback,
                sampleRateHz = sampleRateHz,
                sourceStartSamples = startOffsetSamples,
                sourceTotalSamples = totalSamples,
                onPlaybackStarted = onPlaybackStarted,
                onProgressChanged = onProgressChanged,
            )
        }
        val track =
            createStaticAudioTrack(
                sampleRateHz = sampleRateHz,
                sampleCount = playbackPcm.size,
            )

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
                onPlaybackStarted = onPlaybackStarted,
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
        playbackSpeed: Float = PlaybackSpeedNormal,
        startSampleIndex: Int = 0,
        renderContext: PlaybackRenderContext = PlaybackRenderContext.Empty,
        preferStreamingSpeedAdjustedPcm: Boolean = false,
        onPlaybackStarted: () -> Unit = {},
        onProgressChanged: (Int, Int) -> Unit = { _, _ -> },
    ): PlaybackResult {
        val startOffsetSamples = startSampleIndex.coerceIn(0, totalSamples)
        safeDebugLog(
            AudioPlayerDiagTag,
            "playPcmFile handle=${playback.debugHandleId()} total=$totalSamples sampleRate=$sampleRateHz " +
                "speed=$playbackSpeed start=$startOffsetSamples path=$pcmFilePath",
        )
        if (startOffsetSamples >= totalSamples) {
            return completePlaybackAtEnd(playback, totalSamples, onProgressChanged)
        }
        val resolvedPlaybackSpeed = coercePlaybackSpeed(playbackSpeed)

        if (shouldRenderSpeedAdjustedPcm(resolvedPlaybackSpeed)) {
            val playbackPcm =
                loadPcmFileRangeWithPlaybackSpeedMemoryTrace(
                    handleId = playback.debugHandleId(),
                    playbackMode = renderContext.diagPlaybackMode(),
                    playbackSpeed = resolvedPlaybackSpeed,
                    sourceSampleCount = totalSamples - startOffsetSamples,
                ) {
                    readPcmFileRange(pcmFilePath, startOffsetSamples, totalSamples)
                }
            if (shouldStreamSpeedAdjustedPlayback(playbackPcm.size, resolvedPlaybackSpeed, preferStreamingSpeedAdjustedPcm)) {
                return playStreamingSpeedAdjustedPcm(
                    playback = playback,
                    sourcePcm = playbackPcm,
                    sampleRateHz = sampleRateHz,
                    playbackSpeed = resolvedPlaybackSpeed,
                    sourceStartSamples = startOffsetSamples,
                    sourceTotalSamples = totalSamples,
                    renderContext = renderContext,
                    fileBacked = true,
                    onPlaybackStarted = onPlaybackStarted,
                    onProgressChanged = onProgressChanged,
                )
            }
            val renderTrace =
                PlaybackSpeedMemoryRenderTrace(
                    handleId = playback.debugHandleId(),
                    playbackMode = renderContext.diagPlaybackMode(),
                    playbackSpeed = resolvedPlaybackSpeed,
                    streaming = false,
                    fileBacked = true,
                    sourceSampleCount = playbackPcm.size,
                )
            val renderPlan =
                renderTrace.measureRender {
                    buildPreRenderedSpeedAdjustedRenderPlan(
                        sourcePcm = playbackPcm,
                        sourceStartSamples = startOffsetSamples,
                        sourceTotalSamples = totalSamples,
                        playbackSpeed = resolvedPlaybackSpeed,
                        sampleRateHz = sampleRateHz,
                        context = renderContext,
                    )
                }
            renderTrace.log(
                event = "preRendered",
                rendererType = renderPlan.rendererType,
                renderedSampleCount = renderPlan.renderedPlayback.pcm.size,
            )
            return playRenderedSpeedAdjustedPcm(
                playback = playback,
                renderedPlayback = renderPlan.renderedPlayback,
                sampleRateHz = sampleRateHz,
                sourceStartSamples = startOffsetSamples,
                sourceTotalSamples = totalSamples,
                onPlaybackStarted = onPlaybackStarted,
                onProgressChanged = onProgressChanged,
            )
        }
        val track = createStreamingAudioTrack(sampleRateHz)

        currentPlayback = playback
        playback.track = track
        playback.playbackSpeed = resolvedPlaybackSpeed
        playback.totalSamples = totalSamples
        playback.bufferStartSamples = startOffsetSamples
        playback.bufferedSamples = totalSamples - startOffsetSamples
        return try {
            setPlaybackSpeedSafely(track, resolvedPlaybackSpeed)
            BufferedInputStream(FileInputStream(pcmFilePath), StreamingBufferBytes).use { input ->
                skipPcmBytesFully(input, startOffsetSamples.toLong() * PcmShortBytes.toLong())
                runStreamingPlaybackLoop(
                    playback = playback,
                    track = track,
                    input = input,
                    sampleRateHz = sampleRateHz,
                    playbackStartOffsetSamples = startOffsetSamples,
                    reportedTotalSamples = totalSamples,
                    onPlaybackStarted = onPlaybackStarted,
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
        val relativePosition =
            playback.renderedTimeline
                ?.renderedPositionForSource(clampedPosition)
                ?: (clampedPosition - playback.bufferStartSamples)
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
                if (playback.renderedTimeline == null) {
                    setPlaybackSpeedSafely(track, playback.playbackSpeed)
                }
                safelyPlayTrack(track)
            }
        }
    }

    fun setPlaybackSpeed(playbackSpeed: Float): Boolean {
        val playback = currentPlayback ?: return false
        val resolvedPlaybackSpeed = coercePlaybackSpeed(playbackSpeed)
        if (shouldRenderSpeedAdjustedPcm(resolvedPlaybackSpeed) || playback.renderedTimeline != null) {
            safeDebugLog(
                AudioPlayerDiagTag,
                "setSpeedRequiresRestart handle=${playback.debugHandleId()} from=${playback.playbackSpeed} to=$resolvedPlaybackSpeed",
            )
            return false
        }
        safeDebugLog(
            AudioPlayerDiagTag,
            "setSpeed handle=${playback.debugHandleId()} from=${playback.playbackSpeed} to=$resolvedPlaybackSpeed",
        )
        playback.playbackSpeed = resolvedPlaybackSpeed
        val track =
            playback.track
                ?: run {
                    safeDebugLog(
                        AudioPlayerDiagTag,
                        "setSpeedResult handle=${playback.debugHandleId()} requested=$playbackSpeed stored=${playback.playbackSpeed} " +
                            "applied=true track=false",
                    )
                    return true
                }
        val applied = setPlaybackSpeedSafely(track, resolvedPlaybackSpeed)
        safeDebugLog(
            AudioPlayerDiagTag,
            "setSpeedResult handle=${playback.debugHandleId()} requested=$playbackSpeed stored=${playback.playbackSpeed} " +
                "applied=$applied track=true",
        )
        return applied
    }

    fun stop() {
        currentPlayback?.let(::stop)
    }

    fun stop(playback: PlaybackHandle) {
        playback.let {
            safeDebugLog(
                AudioPlayerDiagTag,
                "stop handle=${it.debugHandleId()} track=${it.track != null} " +
                    "stopRequested=${it.stopRequested} pauseRequested=${it.pauseRequested}",
            )
            it.stopRequested = true
            it.pauseRequested = false
            it.track?.let { track ->
                it.stopFadeInProgress = true
                try {
                    safelyFadeOutAndStopTrack(track)
                } finally {
                    it.stopFadeInProgress = false
                }
            }
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
        awaitStopFadeBeforeRelease(playback, track)
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
        playback.renderedTimeline = null
        playback.stopFadeInProgress = false
        track.release()
    }

    private fun playRenderedSpeedAdjustedPcm(
        playback: PlaybackHandle,
        renderedPlayback: RenderedSpeedAdjustedPcm,
        sampleRateHz: Int,
        sourceStartSamples: Int,
        sourceTotalSamples: Int,
        onPlaybackStarted: () -> Unit,
        onProgressChanged: (Int, Int) -> Unit,
    ): PlaybackResult {
        val renderedPcm = renderedPlayback.pcm
        if (renderedPcm.isEmpty()) {
            return completePlaybackAtEnd(playback, sourceTotalSamples, onProgressChanged)
        }
        val track = createStreamingAudioTrack(sampleRateHz)
        currentPlayback = playback
        playback.track = track
        playback.playbackSpeed = PlaybackSpeedNormal
        playback.totalSamples = sourceTotalSamples
        playback.bufferStartSamples = sourceStartSamples
        playback.bufferedSamples = sourceTotalSamples - sourceStartSamples
        playback.renderedTimeline = renderedPlayback.timeline
        return try {
            setPlaybackSpeedSafely(track, PlaybackSpeedNormal)
            runRenderedPcmStreamingPlaybackLoop(
                playback = playback,
                track = track,
                pcm = renderedPcm,
                sampleRateHz = sampleRateHz,
                onPlaybackStarted = onPlaybackStarted,
                onProgressChanged = { renderedSamples, _ ->
                    val sourceProgress = renderedPlayback.timeline.sourceProgress(renderedSamples)
                    onProgressChanged(sourceProgress, sourceTotalSamples)
                },
            )
        } finally {
            releasePlaybackTrack(playback, track)
        }
    }

    private fun playStreamingSpeedAdjustedPcm(
        playback: PlaybackHandle,
        sourcePcm: ShortArray,
        sampleRateHz: Int,
        playbackSpeed: Float,
        sourceStartSamples: Int,
        sourceTotalSamples: Int,
        renderContext: PlaybackRenderContext,
        fileBacked: Boolean = false,
        onPlaybackStarted: () -> Unit,
        onProgressChanged: (Int, Int) -> Unit,
    ): PlaybackResult {
        val renderTrace =
            PlaybackSpeedMemoryRenderTrace(
                handleId = playback.debugHandleId(),
                playbackMode = renderContext.diagPlaybackMode(),
                playbackSpeed = playbackSpeed,
                streaming = true,
                fileBacked = fileBacked,
                sourceSampleCount = sourcePcm.size,
            )
        val renderPlan =
            renderTrace.measureRender {
                buildStreamingSpeedAdjustedRenderPlan(
                    sourcePcm = sourcePcm,
                    sourceStartSamples = sourceStartSamples,
                    sourceTotalSamples = sourceTotalSamples,
                    playbackSpeed = playbackSpeed,
                    sampleRateHz = sampleRateHz,
                    context = renderContext,
                )
            }
        val renderedTotalSamples = renderPlan.renderedTotalSamples
        if (renderedTotalSamples <= 0 || sourcePcm.isEmpty()) {
            return completePlaybackAtEnd(playback, sourceTotalSamples, onProgressChanged)
        }
        safeDebugLog(
            AudioPlayerDiagTag,
            "streamSpeedAdjustedPcm handle=${playback.debugHandleId()} source=${sourcePcm.size} rendered=$renderedTotalSamples " +
                "speed=$playbackSpeed sampleRate=$sampleRateHz start=$sourceStartSamples total=$sourceTotalSamples",
        )
        val track = createStreamingAudioTrack(sampleRateHz)
        currentPlayback = playback
        playback.track = track
        playback.playbackSpeed = PlaybackSpeedNormal
        playback.totalSamples = sourceTotalSamples
        playback.bufferStartSamples = sourceStartSamples
        playback.bufferedSamples = sourceTotalSamples - sourceStartSamples
        playback.renderedTimeline = renderPlan.timeline
        return try {
            setPlaybackSpeedSafely(track, PlaybackSpeedNormal)
            val result =
                runSpeedAdjustedPcmRenderPlaybackLoop(
                    playback = playback,
                    track = track,
                    sourcePcm = sourcePcm,
                    playbackSpeed = playbackSpeed,
                    sampleRateHz = sampleRateHz,
                    renderedTotalSamples = renderedTotalSamples,
                    renderChunk = { outputStartSamples, outputSampleCount ->
                        renderTrace.measureRender {
                            renderPlan.renderChunk(outputStartSamples, outputSampleCount)
                        }
                    },
                    onPlaybackStarted = onPlaybackStarted,
                    onProgressChanged = { renderedSamples, _ ->
                        onProgressChanged(renderPlan.timeline.sourceProgress(renderedSamples), sourceTotalSamples)
                    },
                )
            renderTrace.log(
                event = "streaming",
                rendererType = renderPlan.rendererType,
                renderedSampleCount = renderedTotalSamples,
            )
            result
        } finally {
            releasePlaybackTrack(playback, track)
        }
    }

    private companion object {
        const val StreamingBufferBytes = PcmStreamingBufferBytes
    }

    private fun completePlaybackAtEnd(
        playback: PlaybackHandle,
        totalSamples: Int,
        onProgressChanged: (Int, Int) -> Unit,
    ): PlaybackResult {
        currentPlayback = playback
        playback.track = null
        playback.totalSamples = totalSamples
        playback.bufferStartSamples = totalSamples
        playback.bufferedSamples = 0
        playback.renderedTimeline = null
        onProgressChanged(totalSamples, totalSamples)
        if (currentPlayback === playback) {
            currentPlayback = null
        }
        playback.stopRequested = false
        playback.pauseRequested = false
        return PlaybackResult.Completed
    }
}

private const val AudioPlayerDiagTag = "AudioPlayerDiag"
private const val PlaybackEdgeFadeDiagTag = "PlaybackEdgeFade"
private const val StopFadeReleaseWaitStepMs = 1L
private const val StopFadeReleaseMaxWaitMs = 40L

private fun AudioPlayer.PlaybackHandle.debugHandleId(): String = Integer.toHexString(System.identityHashCode(this))

private fun awaitStopFadeBeforeRelease(
    playback: AudioPlayer.PlaybackHandle,
    track: AudioTrack,
) {
    if (!playback.stopFadeInProgress || track.playState == AudioTrack.PLAYSTATE_STOPPED) {
        return
    }
    var waitedMs = 0L
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "releaseAwaitFade handle=${playback.debugHandleId()} playState=${track.playState} " +
            "head=${track.safePlaybackHeadPosition() ?: -1} maxWaitMs=$StopFadeReleaseMaxWaitMs",
    )
    while (
        playback.stopFadeInProgress &&
        track.playState != AudioTrack.PLAYSTATE_STOPPED &&
        waitedMs < StopFadeReleaseMaxWaitMs
    ) {
        Thread.sleep(StopFadeReleaseWaitStepMs)
        waitedMs += StopFadeReleaseWaitStepMs
    }
    safeDebugLog(
        PlaybackEdgeFadeDiagTag,
        "releaseAwaitFadeDone handle=${playback.debugHandleId()} playState=${track.playState} " +
            "head=${track.safePlaybackHeadPosition() ?: -1} waitedMs=$waitedMs inProgress=${playback.stopFadeInProgress}",
    )
}
