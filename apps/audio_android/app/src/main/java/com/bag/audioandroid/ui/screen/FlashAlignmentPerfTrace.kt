package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.BuildConfig

internal object FlashAlignmentPerfTrace {
    private const val Tag = "FlashAlignmentPerf"
    private const val ReportIntervalNanos = 500_000_000L

    private var lastReportNanos = 0L
    private var visualPlaying = false
    private var visualSample = 0
    private var rawSample = 0
    private var readoutSample = 0
    private var readoutBit = -1
    private var revealedBit = -1
    private var visualBit = -1
    private var rawBit = -1
    private var fallback = false
    private var bitReadout = false
    private var mode = "unknown"
    private var lyricsPlaying = false
    private var lyricsSample = 0
    private var token = -1
    private var tokenText = "_"
    private var tokenStart = -1
    private var tokenEnd = -1
    private var tokenProgress = "-1.00"
    private var byte = -1
    private var lyricBit = -1
    private var lyricBitOffset = -1
    private var tone = false

    fun recordVisual(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        smoothSample: Float,
        rawSample: Float,
        readoutSample: Float,
        readoutBit: Int?,
        revealedBit: Int,
        visualBit: Int?,
        rawBit: Int?,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        this.mode = mode.name
        visualPlaying = isPlaying
        visualSample = smoothSample.toInt()
        this.rawSample = rawSample.toInt()
        this.readoutSample = readoutSample.toInt()
        this.readoutBit = readoutBit ?: -1
        this.revealedBit = revealedBit
        this.visualBit = visualBit ?: -1
        this.rawBit = rawBit ?: -1
        fallback = usesFallbackTimeline
        bitReadout = hasBitReadout
        maybeReport()
    }

    fun recordLyrics(
        isPlaying: Boolean,
        sample: Int,
        state: FlashAlignmentLyricsState,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        lyricsPlaying = isPlaying
        lyricsSample = sample
        token = state.token
        tokenText = state.tokenText.logSafe()
        tokenStart = state.tokenStart
        tokenEnd = state.tokenEnd
        tokenProgress = "%.2f".format(state.tokenProgress)
        byte = state.byte
        lyricBit = state.bit
        lyricBitOffset = state.bitOffset
        tone = state.tone
    }

    private fun maybeReport() {
        val now = System.nanoTime()
        if (lastReportNanos == 0L) {
            lastReportNanos = now
            return
        }
        if (now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now
        logDebug(
            "mode=$mode visualPlaying=$visualPlaying lyricsPlaying=$lyricsPlaying " +
                "visualSample=$visualSample rawSample=$rawSample readoutSample=$readoutSample " +
                "readoutBit=$readoutBit revealedBit=$revealedBit visualBit=$visualBit rawBit=$rawBit " +
                "fallback=$fallback bitReadout=$bitReadout " +
                "lyricsSample=$lyricsSample token=$token tokenText=$tokenText " +
                "tokenStart=$tokenStart tokenEnd=$tokenEnd tokenProgress=$tokenProgress " +
                "byte=$byte lyricBit=$lyricBit lyricBitOffset=$lyricBitOffset tone=$tone " +
                "sampleDelta=${visualSample - lyricsSample} bitDelta=${readoutBit - lyricBitOffset}",
        )
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

internal data class FlashAlignmentLyricsState(
    val token: Int,
    val tokenText: String,
    val tokenStart: Int,
    val tokenEnd: Int,
    val tokenProgress: Float,
    val byte: Int,
    val bit: Int,
    val bitOffset: Int,
    val tone: Boolean,
)

internal fun flashAlignmentLyricsState(
    followData: com.bag.audioandroid.domain.PayloadFollowViewData,
    displayedSamples: Int,
): FlashAlignmentLyricsState {
    val activeTimelineIndex = activeTextTimelineIndex(followData.textTokenTimeline, displayedSamples)
    val activeTimelineEntry = followData.textTokenTimeline.getOrNull(activeTimelineIndex)
    val activeTokenIndex = activeTimelineEntry?.tokenIndex ?: -1
    val rawDisplayUnitsByToken = followData.textRawDisplayUnits.groupBy { it.tokenIndex }
    val byte =
        activeByteIndexWithinToken(
            activeTextIndex = activeTokenIndex,
            displayedSamples = displayedSamples,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val bitPosition =
        activeBitPositionWithinByte(
            activeTextIndex = activeTokenIndex,
            activeByteIndexWithinToken = byte,
            displayedSamples = displayedSamples,
            followData = followData,
            rawDisplayUnitsByToken = rawDisplayUnitsByToken,
        )
    val bitOffset =
        byte
            .takeIf { it >= 0 && bitPosition.bitIndexWithinByte >= 0 }
            ?.let { it * 8 + bitPosition.bitIndexWithinByte }
            ?: -1
    val tokenProgress =
        activeTimelineEntry
            ?.takeIf { it.sampleCount > 0 }
            ?.let { ((displayedSamples - it.startSample).toFloat() / it.sampleCount.toFloat()).coerceIn(0f, 1f) }
            ?: -1f
    return FlashAlignmentLyricsState(
        token = activeTokenIndex,
        tokenText = followData.textTokens.getOrNull(activeTokenIndex).orEmpty(),
        tokenStart = activeTimelineEntry?.startSample ?: -1,
        tokenEnd = activeTimelineEntry?.let { it.startSample + it.sampleCount } ?: -1,
        tokenProgress = tokenProgress,
        byte = byte,
        bit = bitPosition.bitIndexWithinByte,
        bitOffset = bitOffset,
        tone = bitPosition.isToneActive,
    )
}
