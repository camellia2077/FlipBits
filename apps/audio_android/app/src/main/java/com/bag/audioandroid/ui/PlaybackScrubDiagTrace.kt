package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.PlaybackDisplayMode
import com.bag.audioandroid.ui.screen.PlaybackFollowViewMode
import com.bag.audioandroid.ui.screen.PlaybackVisualizationRoute
import com.bag.audioandroid.ui.state.PlaybackUiState
import com.bag.audioandroid.util.safeDebugLog
import java.util.concurrent.ConcurrentHashMap

internal object PlaybackScrubDiagTrace {
    private val lastProgressLogNanosBySource = ConcurrentHashMap<String, Long>()
    private val lastDisplayLogNanosByKey = ConcurrentHashMap<String, Long>()

    fun event(
        name: String,
        source: AudioPlaybackSource,
        playback: PlaybackUiState,
        detail: String = "",
    ) {
        safeDebugLog(
            Tag,
            "event=$name source=${source.diagName()} ${playback.diagFields()} $detail".trim(),
        )
    }

    fun progress(
        eventName: String = "progress",
        source: AudioPlaybackSource,
        playback: PlaybackUiState,
        playedSamples: Int,
        totalSamples: Int,
    ) {
        val key = "$eventName:${source.diagName()}"
        if (!shouldLog(lastProgressLogNanosBySource, key, ProgressLogIntervalNanos)) {
            return
        }
        safeDebugLog(
            Tag,
            "event=$eventName source=${source.diagName()} ${playback.diagFields()} " +
                "reportedPlayed=$playedSamples reportedTotal=$totalSamples",
        )
    }

    fun display(
        displayMode: PlaybackDisplayMode,
        visualizationRoute: PlaybackVisualizationRoute,
        displayedSamples: Int,
        visualDisplayedSamples: Int,
        followSectionDisplayedSamples: Int,
        isPlaying: Boolean,
        isScrubbing: Boolean,
        playbackSpeed: Float,
        activeTokenIndex: Int,
    ) {
        val key = "display:${displayMode.name}:${visualizationRoute::class.simpleName.orEmpty()}"
        if (!shouldLog(lastDisplayLogNanosByKey, key, DisplayLogIntervalNanos)) {
            return
        }
        safeDebugLog(
            Tag,
            "event=display mode=${displayMode.name.lowercase()} route=${visualizationRoute::class.simpleName.orEmpty()} " +
                "playing=$isPlaying scrubbing=$isScrubbing speed=$playbackSpeed " +
                "displayed=$displayedSamples visual=$visualDisplayedSamples follow=$followSectionDisplayedSamples token=$activeTokenIndex",
        )
    }

    fun follow(
        transportMode: TransportModeOption?,
        followViewMode: PlaybackFollowViewMode,
        displayedSamples: Int,
        isPlaying: Boolean,
        activeTokenIndex: Int,
        activeByteIndex: Int,
        activeBitIndex: Int,
        activeBitCount: Int,
        isActiveBitTone: Boolean,
    ) {
        val key = "follow:${transportMode?.wireName ?: "unknown"}:${followViewMode.name}"
        if (!shouldLog(lastDisplayLogNanosByKey, key, DisplayLogIntervalNanos)) {
            return
        }
        safeDebugLog(
            Tag,
            "event=follow transport=${transportMode?.wireName ?: "unknown"} " +
                "mode=${followViewMode.name.lowercase()} playing=$isPlaying displayed=$displayedSamples " +
                "token=$activeTokenIndex byte=$activeByteIndex bit=$activeBitIndex bitCount=$activeBitCount " +
                "tone=$isActiveBitTone",
        )
    }

    private fun shouldLog(
        lastLogNanosByKey: ConcurrentHashMap<String, Long>,
        key: String,
        intervalNanos: Long,
    ): Boolean {
        val now = System.nanoTime()
        val previous = lastLogNanosByKey[key]
        if (previous != null && now - previous < intervalNanos) {
            return false
        }
        lastLogNanosByKey[key] = now
        return true
    }

    private fun AudioPlaybackSource.diagName(): String =
        when (this) {
            is AudioPlaybackSource.Generated -> "generated:${mode.wireName}"
            is AudioPlaybackSource.Saved -> "saved:$itemId"
        }

    private fun PlaybackUiState.diagFields(): String =
        "phase=${phase.name.lowercase()} playing=$isPlaying paused=$isPaused scrubbing=$isScrubbing " +
            "played=$playedSamples displayed=$displayedSamples scrub=$scrubPreviewSamples " +
            "total=$totalSamples sampleRate=$sampleRateHz resumeAfterScrub=$resumeAfterScrub"

    private const val Tag = "PlaybackScrubDiag"
    private const val ProgressLogIntervalNanos = 250_000_000L
    private const val DisplayLogIntervalNanos = 250_000_000L
}
