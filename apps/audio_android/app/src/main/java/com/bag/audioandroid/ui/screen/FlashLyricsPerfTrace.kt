package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry

internal object FlashLyricsPerfTrace {
    private const val Tag = "FlashLyricsPerf"
    private const val MinLogStepSamples = 22_050
    private var lastLogBucket = Int.MIN_VALUE
    private var lastTokenIndex = Int.MIN_VALUE
    private var lastPlaying = false

    fun record(
        followData: PayloadFollowViewData,
        displayedSamples: Int,
        isPlaying: Boolean,
        activeTimelineEntry: TextFollowTimelineEntry?,
        activeTokenIndex: Int,
        activeLineIndex: Int,
        displayLineRange: DisplayTokenLineRange?,
        sourceLineRanges: List<TextFollowLineTokenRangeViewData>,
        activeByteIndexWithinToken: Int,
        activeBitPosition: ActiveBitPosition,
    ) {
        if (!BuildConfig.DEBUG || !followData.textFollowAvailable) {
            return
        }
        val bucket = displayedSamples / MinLogStepSamples
        if (bucket == lastLogBucket && activeTokenIndex == lastTokenIndex && isPlaying == lastPlaying) {
            return
        }
        lastLogBucket = bucket
        lastTokenIndex = activeTokenIndex
        lastPlaying = isPlaying

        val entryStart = activeTimelineEntry?.startSample ?: -1
        val entryEnd = activeTimelineEntry?.let { it.startSample + it.sampleCount } ?: -1
        val entryProgress =
            activeTimelineEntry
                ?.takeIf { it.sampleCount > 0 }
                ?.let { ((displayedSamples - it.startSample).toFloat() / it.sampleCount.toFloat()).coerceIn(0f, 1f) }
                ?: -1f
        val tokenText = followData.textTokens.getOrNull(activeTokenIndex).orEmpty()
        val sourceLineIndex =
            sourceLineRanges.indexOfFirst { lineRange ->
                activeTokenIndex >= lineRange.tokenBeginIndex &&
                    activeTokenIndex < lineRange.tokenBeginIndex + lineRange.tokenCount
            }
        val sourceLine = followData.lyricLines.getOrNull(sourceLineIndex).orEmpty()
        logDebug(
            "playing=$isPlaying sample=$displayedSamples token=$activeTokenIndex tokenText=${tokenText.logSafe()} " +
                "tokenStart=$entryStart tokenEnd=$entryEnd tokenProgress=${"%.2f".format(entryProgress)} " +
                "displayLine=$activeLineIndex displayRange=${displayLineRange.rangeText()} " +
                "sourceLine=$sourceLineIndex sourceLineText=${sourceLine.logSafe()} " +
                "byte=$activeByteIndexWithinToken bit=${activeBitPosition.bitIndexWithinByte} " +
                "tone=${activeBitPosition.isToneActive} textTokens=${followData.textTokens.size} " +
                "lineRanges=${sourceLineRanges.size}",
        )
    }

    private fun DisplayTokenLineRange?.rangeText(): String =
        this?.let { "${it.tokenBeginIndex}-${it.tokenBeginIndex + it.tokenCount}" } ?: "-"

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
