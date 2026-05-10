package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData

internal object MiniAlignmentPerfTrace {
    private const val Tag = "MiniAlignmentPerf"
    private const val ReportIntervalNanos = 500_000_000L

    private var lastReportNanos = 0L

    fun record(
        followData: PayloadFollowViewData,
        isPlaying: Boolean,
        visualSample: Int,
        lyricsSample: Int,
        frameSamples: Int,
        speed: String,
    ) {
        if (!BuildConfig.DEBUG || !followData.followAvailable || followData.binaryGroupTimeline.isEmpty()) {
            return
        }
        val now = System.nanoTime()
        if (lastReportNanos == 0L) {
            lastReportNanos = now
            return
        }
        if (now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now

        val visualEntry = miniActiveBinaryGroup(followData.binaryGroupTimeline, visualSample)
        val lyricsState = flashAlignmentLyricsState(followData = followData, displayedSamples = lyricsSample)
        logDebug(
            "playing=$isPlaying speed=$speed frameSamples=$frameSamples " +
                "visualSample=$visualSample lyricsSample=$lyricsSample sampleDelta=${visualSample - lyricsSample} " +
                "visualGroup=${visualEntry?.groupIndex ?: -1} visualBitOffset=${visualEntry?.bitOffset ?: -1} " +
                "visualBitCount=${visualEntry?.bitCount ?: -1} visualStart=${visualEntry?.startSample ?: -1} " +
                "visualEnd=${visualEntry?.let { it.startSample + it.sampleCount } ?: -1} " +
                "visualActive=${visualEntry != null} " +
                "token=${lyricsState.token} tokenText=${lyricsState.tokenText.logSafe()} " +
                "tokenStart=${lyricsState.tokenStart} tokenEnd=${lyricsState.tokenEnd} " +
                "tokenProgress=${"%.2f".format(lyricsState.tokenProgress)} " +
                "byte=${lyricsState.byte} lyricBit=${lyricsState.bit} " +
                "lyricBitOffset=${lyricsState.bitOffset} tone=${lyricsState.tone}",
        )
    }

    private fun miniActiveBinaryGroup(
        entries: List<PayloadFollowBinaryGroupTimelineEntry>,
        sample: Int,
    ): PayloadFollowBinaryGroupTimelineEntry? =
        entries.firstOrNull { entry ->
            sample >= entry.startSample && sample < entry.startSample + entry.sampleCount
        }

    private fun String.logSafe(): String =
        replace('\n', ' ')
            .replace('\r', ' ')
            .take(64)
            .ifBlank { "_" }

    private fun logDebug(message: String) {
        try {
            Log.d(Tag, message)
        } catch (_: Throwable) {
            // JVM unit tests use the Android stub jar, where Log.d is not implemented.
        }
    }
}
