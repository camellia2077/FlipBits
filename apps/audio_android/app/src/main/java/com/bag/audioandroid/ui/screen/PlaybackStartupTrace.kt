package com.bag.audioandroid.ui.screen

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption

internal object PlaybackStartupTrace {
    private const val Tag = "PlaybackStartupTrace"
    private const val CaptureWindowMs = 1_800L
    private val enabled =
        BuildConfig.DEBUG ||
            BuildConfig.BUILD_TYPE == "staging" ||
            BuildConfig.BUILD_TYPE == "diagnosticRelease"

    private val flashState = TraceState()
    private val tokenState = TraceState()

    fun recordFlashVisual(
        mode: FlashSignalVisualizationMode,
        isPlaying: Boolean,
        sampleRateHz: Int,
        rawSample: Float,
        smoothSample: Float,
        viewportStartSample: Float,
        windowStartSample: Int,
        windowEndSample: Int,
        totalSamples: Int,
        readoutBit: Int?,
        visualBit: Int?,
        rawBit: Int?,
        usesFallbackTimeline: Boolean,
        hasBitReadout: Boolean,
    ) {
        if (!enabled) {
            return
        }
        val key = "flash:${mode.name}:sr=$sampleRateHz:total=$totalSamples"
        val now = android.os.SystemClock.elapsedRealtime()
        val started = flashState.updatePlaying(key = key, isPlaying = isPlaying, nowMs = now)
        if (!flashState.shouldLog(now)) {
            return
        }
        val rawDelta = flashState.deltaFromPreviousRaw(rawSample)
        val smoothDelta = flashState.deltaFromPreviousSmooth(smoothSample)
        val viewportDelta = flashState.deltaFromPreviousViewport(viewportStartSample)
        logDebug(
            "surface=flash event=${if (started) "start" else "tick"} session=${flashState.session} " +
                "elapsedMs=${flashState.elapsedMs(now)} mode=${mode.name} playing=$isPlaying " +
                "raw=${rawSample.toInt()} rawDelta=${rawDelta.toInt()} smooth=${smoothSample.toInt()} " +
                "smoothDelta=${smoothDelta.toInt()} viewport=${viewportStartSample.toInt()} " +
                "viewportDelta=${viewportDelta.toInt()} window=[$windowStartSample,$windowEndSample) " +
                "readoutBit=${readoutBit ?: -1} visualBit=${visualBit ?: -1} rawBit=${rawBit ?: -1} " +
                "fallback=$usesFallbackTimeline bitReadout=$hasBitReadout total=$totalSamples " +
                "rawBack=${rawDelta < -0.5f} smoothBack=${smoothDelta < -0.5f} viewportBack=${viewportDelta < -0.5f}",
        )
    }

    fun recordTokenFollow(
        followData: PayloadFollowViewData,
        presentationState: PlaybackFollowPresentationState,
        displayedSamples: Int,
        isPlaying: Boolean,
        transportMode: TransportModeOption?,
    ) {
        if (!enabled || transportMode != TransportModeOption.Ultra) {
            return
        }
        val mode = presentationState.followViewMode
        if (mode != PlaybackFollowViewMode.Binary && mode != PlaybackFollowViewMode.Hex) {
            return
        }
        val key =
            "token:${mode.name}:total=${followData.totalPcmSampleCount}:" +
                "tokens=${followData.textTokens.size}:units=${followData.textRawDisplayUnits.size}"
        val now = android.os.SystemClock.elapsedRealtime()
        val started = tokenState.updatePlaying(key = key, isPlaying = isPlaying, nowMs = now)
        if (!tokenState.shouldLog(now)) {
            return
        }
        val activeUnit =
            presentationState.rawDisplayUnitsByToken[presentationState.activeTextIndex]
                .orEmpty()
                .firstOrNull { unit -> unit.byteIndexWithinToken == presentationState.activeByteIndexWithinToken }
        val globalBit =
            activeUnit
                ?.takeIf { presentationState.activeBitIndexWithinByte >= 0 }
                ?.let { it.byteOffset * 8 + presentationState.activeBitIndexWithinByte }
                ?: -1
        val sampleDelta = tokenState.deltaFromPreviousSample(displayedSamples)
        val tokenDelta = tokenState.deltaFromPreviousIndex(presentationState.activeTextIndex)
        val byteDelta = tokenState.deltaFromPreviousByte(presentationState.activeByteIndexWithinToken)
        logDebug(
            "surface=token event=${if (started) "start" else "tick"} session=${tokenState.session} " +
                "elapsedMs=${tokenState.elapsedMs(now)} mode=${mode.name} playing=$isPlaying " +
                "sample=$displayedSamples sampleDelta=$sampleDelta token=${presentationState.activeTextIndex} " +
                "tokenDelta=$tokenDelta byte=${presentationState.activeByteIndexWithinToken} byteDelta=$byteDelta " +
                "bit=${presentationState.activeBitIndexWithinByte} globalBit=$globalBit tone=${presentationState.isActiveBitTone} " +
                "unitStart=${activeUnit?.startSample ?: -1} unitEnd=${activeUnit?.let { it.startSample + it.sampleCount } ?: -1} " +
                "hex=${activeUnit?.hexText.orEmpty().ifBlank { "_" }} bin=${activeUnit?.binaryText.orEmpty().ifBlank { "_" }} " +
                "sampleBack=${sampleDelta < 0} tokenBack=${tokenDelta < 0} byteBack=${byteDelta < 0}",
        )
    }

    private fun logDebug(message: String) {
        try {
            Log.d(Tag, message)
        } catch (_: Throwable) {
            // JVM unit tests use the Android stub jar, where Log.d is not implemented.
        }
    }

    private class TraceState {
        var session: Int = 0
            private set

        private var key: String? = null
        private var wasPlaying = false
        private var captureStartedMs = 0L
        private var captureUntilMs = 0L
        private var previousRawSample: Float? = null
        private var previousSmoothSample: Float? = null
        private var previousViewportSample: Float? = null
        private var previousDisplayedSample: Int? = null
        private var previousIndex: Int? = null
        private var previousByte: Int? = null

        fun updatePlaying(
            key: String,
            isPlaying: Boolean,
            nowMs: Long,
        ): Boolean {
            val keyChanged = this.key != key
            if (keyChanged) {
                resetForKey(key)
            }
            val started = isPlaying && (!wasPlaying || keyChanged)
            if (started) {
                session += 1
                captureStartedMs = nowMs
                captureUntilMs = nowMs + CaptureWindowMs
                previousRawSample = null
                previousSmoothSample = null
                previousViewportSample = null
                previousDisplayedSample = null
                previousIndex = null
                previousByte = null
            }
            wasPlaying = isPlaying
            return started
        }

        fun shouldLog(nowMs: Long): Boolean = captureUntilMs > 0L && nowMs <= captureUntilMs

        fun elapsedMs(nowMs: Long): Long = (nowMs - captureStartedMs).coerceAtLeast(0L)

        fun deltaFromPreviousRaw(value: Float): Float {
            val delta = previousRawSample?.let { value - it } ?: 0f
            previousRawSample = value
            return delta
        }

        fun deltaFromPreviousSmooth(value: Float): Float {
            val delta = previousSmoothSample?.let { value - it } ?: 0f
            previousSmoothSample = value
            return delta
        }

        fun deltaFromPreviousViewport(value: Float): Float {
            val delta = previousViewportSample?.let { value - it } ?: 0f
            previousViewportSample = value
            return delta
        }

        fun deltaFromPreviousSample(value: Int): Int {
            val delta = previousDisplayedSample?.let { value - it } ?: 0
            previousDisplayedSample = value
            return delta
        }

        fun deltaFromPreviousIndex(value: Int): Int {
            val delta = previousIndex?.let { value - it } ?: 0
            previousIndex = value
            return delta
        }

        fun deltaFromPreviousByte(value: Int): Int {
            val delta = previousByte?.let { value - it } ?: 0
            previousByte = value
            return delta
        }

        private fun resetForKey(newKey: String) {
            key = newKey
            wasPlaying = false
            captureStartedMs = 0L
            captureUntilMs = 0L
            previousRawSample = null
            previousSmoothSample = null
            previousViewportSample = null
            previousDisplayedSample = null
            previousIndex = null
            previousByte = null
        }
    }
}
