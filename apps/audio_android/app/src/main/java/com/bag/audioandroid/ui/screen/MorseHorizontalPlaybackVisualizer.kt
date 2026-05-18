package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun MorseHorizontalPlaybackVisualizer(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    frameSamples: Int,
    sampleRateHz: Int,
    isPlaying: Boolean,
    playbackSpeed: Float = 1f,
    isScrubbing: Boolean = false,
    showPerfOverlay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!followData.followAvailable || followData.binaryGroupTimeline.isEmpty() || frameSamples <= 0) {
        return
    }

    val visualTokens = appThemeVisualTokens()
    val toneEntries =
        remember(followData.binaryGroupTimeline) {
            followData.binaryGroupTimeline.sortedBy(PayloadFollowBinaryGroupTimelineEntry::startSample)
        }
    val totalSamples = followData.totalPcmSampleCount.coerceAtLeast(1)
    val rawCurrentSample = displayedSamples.coerceIn(0, totalSamples)
    val currentSample = rawCurrentSample
    val windowSamples =
        remember(frameSamples) {
            (frameSamples * MorseHorizontalWindowFrameCount).coerceAtLeast(frameSamples * MorseHorizontalMinimumFrameCount)
        }
    val window =
        remember(currentSample, windowSamples) {
            resolveMorseHorizontalWindow(
                currentSample = currentSample,
                totalSamples = totalSamples,
                windowSamples = windowSamples,
            )
        }
    val visibleToneEntries =
        remember(toneEntries, window.startSample, window.endSample) {
            toneEntries.count { entry ->
                val entryStart = entry.startSample
                val entryEnd = entry.startSample + entry.sampleCount
                entryEnd >= window.startSample && entryStart <= window.endSample
            }
        }

    LaunchedEffect(showPerfOverlay) {
        MiniVisualPerfTrace.setEnabled(showPerfOverlay)
    }
    if (showPerfOverlay) {
        MiniVisualPerfTrace.recordCompose(
            rawSample = rawCurrentSample,
            smoothSample = currentSample,
            sampleRateHz = sampleRateHz,
            windowStartSample = window.startSample,
            windowEndSample = window.endSample,
            visibleSamples = window.sampleCount,
            totalSamples = totalSamples,
            visibleToneEntries = visibleToneEntries,
        )
    }
    MiniHorizontalGapTrace.record(
        toneEntries = toneEntries,
        rawSample = rawCurrentSample,
        smoothSample = currentSample,
        isPlaying = isPlaying,
        sampleRateHz = sampleRateHz,
    )

    val toneColor = MaterialTheme.colorScheme.primary
    val inactiveToneColor = visualTokens.visualizationInactiveToneColor
    val playheadColor = MaterialTheme.colorScheme.onPrimaryContainer
    val backgroundColor = visualTokens.visualizationBaseBackgroundColor

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .testTag("morse-horizontal-visualizer"),
        ) {
            val drawStartNanos = if (showPerfOverlay) System.nanoTime() else 0L
            drawRoundRect(
                color = backgroundColor,
                size = size,
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            )

            val leftPadding = 16.dp.toPx()
            val rightPadding = 16.dp.toPx()
            val topPadding = 18.dp.toPx()
            val bottomPadding = 18.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val visibleSamples = window.sampleCount.coerceAtLeast(1)
            val centerY = topPadding + innerHeight / 2f
            val dotHeightPx = (MorseElementHeight * 1.35f).toPx().coerceAtMost(innerHeight * 0.46f)
            val dashHeightPx = (MorseElementHeight * 1.7f).toPx().coerceAtMost(innerHeight * 0.64f)

            toneEntries.forEach { entry ->
                val entryStart = entry.startSample
                val entryEnd = entry.startSample + entry.sampleCount
                if (entryEnd < window.startSample || entryStart > window.endSample) {
                    return@forEach
                }
                val visibleStart = entryStart.coerceAtLeast(window.startSample)
                val visibleEnd = entryEnd.coerceAtMost(window.endSample)
                val isDash = entry.sampleCount >= frameSamples * MorseHorizontalDashUnitThreshold
                val blockHeight = if (isDash) dashHeightPx else dotHeightPx
                val fullSegmentMinWidthPx =
                    morseElementVisualWidthPx(
                        isDash = isDash,
                        density = this,
                    )
                val rawPlayedWidthPx =
                    sampleSpanToPx(
                        startSample = visibleStart,
                        endSample = visibleEnd.coerceAtMost(currentSample),
                        window = window,
                        visibleSamples = visibleSamples,
                        innerWidth = innerWidth,
                    )
                val rawFutureWidthPx =
                    sampleSpanToPx(
                        startSample = visibleStart.coerceAtLeast(currentSample),
                        endSample = visibleEnd,
                        window = window,
                        visibleSamples = visibleSamples,
                        innerWidth = innerWidth,
                    )
                val playedMinWidthPx =
                    if (rawPlayedWidthPx > 0f && visibleStart == entryStart && visibleEnd <= currentSample) {
                        fullSegmentMinWidthPx
                    } else {
                        0f
                    }
                val futureMinWidthPx =
                    if (rawFutureWidthPx > 0f && visibleEnd == entryEnd && visibleStart >= currentSample) {
                        fullSegmentMinWidthPx
                    } else {
                        0f
                    }
                val highlightWholeCurrentSegment = currentSample in entryStart until entryEnd
                if (highlightWholeCurrentSegment) {
                    drawHorizontalMorseSegmentPart(
                        visibleStart = visibleStart,
                        visibleEnd = visibleEnd,
                        window = window,
                        visibleSamples = visibleSamples,
                        leftPadding = leftPadding,
                        innerWidth = innerWidth,
                        centerY = centerY,
                        blockHeight = blockHeight,
                        minWidthPx = fullSegmentMinWidthPx,
                        color = toneColor.copy(alpha = if (isPlaying) 0.88f else 0.72f),
                    )
                    if (showPerfOverlay && currentSample in entryStart..entryEnd) {
                        MiniHorizontalGeometryTrace.record(
                            symbol = if (isDash) '-' else '.',
                            sampleRateHz = sampleRateHz,
                            currentSample = currentSample,
                            entryStart = entryStart,
                            entryEnd = entryEnd,
                            rawPlayedWidthPx = rawPlayedWidthPx,
                            rawFutureWidthPx = rawFutureWidthPx,
                            playedWidthPx = rawPlayedWidthPx.coerceAtLeast(playedMinWidthPx),
                            futureWidthPx = rawFutureWidthPx.coerceAtLeast(futureMinWidthPx),
                            playedClipped = playedMinWidthPx <= 0f,
                            futureClipped = futureMinWidthPx <= 0f,
                        )
                    }
                    return@forEach
                }

                drawHorizontalMorseSegmentPart(
                    visibleStart = visibleStart,
                    visibleEnd = visibleEnd.coerceAtMost(currentSample),
                    window = window,
                    visibleSamples = visibleSamples,
                    leftPadding = leftPadding,
                    innerWidth = innerWidth,
                    centerY = centerY,
                    blockHeight = blockHeight,
                    minWidthPx = playedMinWidthPx,
                    color = toneColor.copy(alpha = if (isPlaying) 0.88f else 0.72f),
                )
                drawHorizontalMorseSegmentPart(
                    visibleStart = visibleStart.coerceAtLeast(currentSample),
                    visibleEnd = visibleEnd,
                    window = window,
                    visibleSamples = visibleSamples,
                    leftPadding = leftPadding,
                    innerWidth = innerWidth,
                    centerY = centerY,
                    blockHeight = blockHeight,
                    minWidthPx = futureMinWidthPx,
                    color = inactiveToneColor.copy(alpha = MorseInactiveToneAlpha),
                )
                if (showPerfOverlay && currentSample in entryStart..entryEnd) {
                    MiniHorizontalGeometryTrace.record(
                        symbol = if (isDash) '-' else '.',
                        sampleRateHz = sampleRateHz,
                        currentSample = currentSample,
                        entryStart = entryStart,
                        entryEnd = entryEnd,
                        rawPlayedWidthPx = rawPlayedWidthPx,
                        rawFutureWidthPx = rawFutureWidthPx,
                        playedWidthPx = rawPlayedWidthPx.coerceAtLeast(playedMinWidthPx),
                        futureWidthPx = rawFutureWidthPx.coerceAtLeast(futureMinWidthPx),
                        playedClipped = playedMinWidthPx <= 0f,
                        futureClipped = futureMinWidthPx <= 0f,
                    )
                }
            }

            val playheadX =
                leftPadding +
                    ((currentSample - window.startSample).toFloat() / visibleSamples.toFloat()) * innerWidth
            drawLine(
                color = playheadColor.copy(alpha = 0.80f),
                start = Offset(playheadX, topPadding),
                end = Offset(playheadX, size.height - bottomPadding),
                strokeWidth = 2.dp.toPx(),
            )
            if (showPerfOverlay) {
                MiniVisualPerfTrace.recordDraw(
                    drawDurationNanos = System.nanoTime() - drawStartNanos,
                    playheadXPx = playheadX,
                    rawSample = rawCurrentSample,
                    smoothSample = currentSample,
                    sampleRateHz = sampleRateHz,
                    windowStartSample = window.startSample,
                    windowEndSample = window.endSample,
                    visibleSamples = visibleSamples,
                    totalSamples = totalSamples,
                    visibleToneEntries = visibleToneEntries,
                )
            }
        }
        if (showPerfOverlay) {
            MiniVisualPerfOverlay(
                snapshot = MiniVisualPerfTrace.snapshot(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalMorseSegmentPart(
    visibleStart: Int,
    visibleEnd: Int,
    window: MorseTimelineWindow,
    visibleSamples: Int,
    leftPadding: Float,
    innerWidth: Float,
    centerY: Float,
    blockHeight: Float,
    minWidthPx: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    if (visibleEnd <= visibleStart) {
        return
    }
    val x =
        leftPadding +
            ((visibleStart - window.startSample).toFloat() / visibleSamples.toFloat()) * innerWidth
    val width = sampleSpanToPx(visibleStart, visibleEnd, window, visibleSamples, innerWidth)
    val cornerRadius = blockHeight / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(x, centerY - blockHeight / 2f),
        size = Size(width.coerceAtLeast(minWidthPx), blockHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
}

private fun sampleSpanToPx(
    startSample: Int,
    endSample: Int,
    window: MorseTimelineWindow,
    visibleSamples: Int,
    innerWidth: Float,
): Float {
    if (endSample <= startSample) {
        return 0f
    }
    return (((endSample - startSample).coerceAtLeast(1)).toFloat() / visibleSamples.toFloat()) * innerWidth
}

private fun morseElementVisualWidthPx(
    isDash: Boolean,
    density: Density,
): Float =
    with(density) {
        morseElementVisualWidthDp(if (isDash) '-' else '.').toPx()
    }

internal fun resolveMorseHorizontalWindow(
    currentSample: Int,
    totalSamples: Int,
    windowSamples: Int,
): MorseTimelineWindow {
    val safeWindowSamples = windowSamples.coerceAtLeast(1)
    val unclampedStart = currentSample - (safeWindowSamples * MorseHorizontalPlayheadAnchorRatio).toInt()
    return MorseTimelineWindow(
        startSample = unclampedStart,
        sampleCount = safeWindowSamples,
    )
}

private const val MorseHorizontalDashUnitThreshold = 3
private const val MorseHorizontalPlayheadAnchorRatio = 0.50f
private const val MorseHorizontalWindowFrameCount = 40
private const val MorseHorizontalMinimumFrameCount = 16

private object MiniHorizontalGapTrace {
    private const val Tag = "MiniHorizontalGap"
    private const val ReportIntervalNanos = 250_000_000L
    private var lastReportNanos = 0L

    fun record(
        toneEntries: List<PayloadFollowBinaryGroupTimelineEntry>,
        rawSample: Int,
        smoothSample: Int,
        isPlaying: Boolean,
        sampleRateHz: Int,
    ) {
        if (!BuildConfig.DEBUG || toneEntries.isEmpty()) {
            return
        }
        val now = System.nanoTime()
        if (lastReportNanos != 0L && now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now
        val rawGap = gapAtSample(toneEntries, rawSample)
        val smoothGap = gapAtSample(toneEntries, smoothSample)
        try {
            Log.d(
                Tag,
                "playing=$isPlaying " +
                    "rawSample=$rawSample smoothSample=$smoothSample " +
                    "rawMs=${samplesToMs(rawSample, sampleRateHz)} smoothMs=${samplesToMs(smoothSample, sampleRateHz)} " +
                    "rawGap=${rawGap.format()} smoothGap=${smoothGap.format()}",
            )
        } catch (_: Throwable) {
        }
    }

    private fun gapAtSample(
        toneEntries: List<PayloadFollowBinaryGroupTimelineEntry>,
        sample: Int,
    ): MorseGapState {
        var previousEnd = 0
        toneEntries.forEachIndexed { index, entry ->
            val entryStart = entry.startSample
            val entryEnd = entry.startSample + entry.sampleCount
            if (sample < entryStart) {
                return MorseGapState(
                    isGap = sample >= previousEnd,
                    previousEndSample = previousEnd,
                    nextStartSample = entryStart,
                    nextGroupIndex = entry.groupIndex,
                )
            }
            if (sample < entryEnd) {
                return MorseGapState(
                    isGap = false,
                    previousEndSample = previousEnd,
                    nextStartSample = entryStart,
                    nextGroupIndex = entry.groupIndex,
                )
            }
            previousEnd = entryEnd
            if (index == toneEntries.lastIndex) {
                return MorseGapState(
                    isGap = sample >= entryEnd,
                    previousEndSample = entryEnd,
                    nextStartSample = -1,
                    nextGroupIndex = -1,
                )
            }
        }
        return MorseGapState(
            isGap = true,
            previousEndSample = previousEnd,
            nextStartSample = -1,
            nextGroupIndex = -1,
        )
    }

    private fun MorseGapState.format(): String =
        "${if (isGap) "gap" else "tone"}(prevEnd=$previousEndSample nextStart=$nextStartSample nextGroup=$nextGroupIndex)"

    private fun samplesToMs(
        sample: Int,
        sampleRateHz: Int,
    ): String =
        if (sampleRateHz <= 0) {
            "0.0"
        } else {
            "%.1f".format(sample * 1000f / sampleRateHz.toFloat())
        }
}

private data class MorseGapState(
    val isGap: Boolean,
    val previousEndSample: Int,
    val nextStartSample: Int,
    val nextGroupIndex: Int,
)

private object MiniHorizontalGeometryTrace {
    private const val Tag = "MiniHorizontalGeometry"
    private const val ReportIntervalNanos = 250_000_000L
    private var lastReportNanos = 0L

    fun record(
        symbol: Char,
        sampleRateHz: Int,
        currentSample: Int,
        entryStart: Int,
        entryEnd: Int,
        rawPlayedWidthPx: Float,
        rawFutureWidthPx: Float,
        playedWidthPx: Float,
        futureWidthPx: Float,
        playedClipped: Boolean,
        futureClipped: Boolean,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val now = System.nanoTime()
        if (lastReportNanos != 0L && now - lastReportNanos < ReportIntervalNanos) {
            return
        }
        lastReportNanos = now
        val remainingSamples = (entryEnd - currentSample).coerceAtLeast(0)
        try {
            Log.d(
                Tag,
                "symbol=$symbol " +
                    "sample=$currentSample entry=[$entryStart,$entryEnd) " +
                    "remainingMs=${samplesToMs(remainingSamples, sampleRateHz)} " +
                    "playedRawPx=${"%.2f".format(rawPlayedWidthPx)} playedPx=${"%.2f".format(playedWidthPx)} " +
                    "futureRawPx=${"%.2f".format(rawFutureWidthPx)} futurePx=${"%.2f".format(futureWidthPx)} " +
                    "playedClipped=$playedClipped futureClipped=$futureClipped",
            )
        } catch (_: Throwable) {
        }
    }

    private fun samplesToMs(
        samples: Int,
        sampleRateHz: Int,
    ): String =
        if (sampleRateHz <= 0) {
            "0.0"
        } else {
            "%.1f".format(samples * 1000f / sampleRateHz.toFloat())
        }
}
