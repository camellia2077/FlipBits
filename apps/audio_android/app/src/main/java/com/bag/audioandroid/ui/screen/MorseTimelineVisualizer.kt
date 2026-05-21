package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.roundToInt

internal data class MorseTimelineWindow(
    val startSample: Int,
    val sampleCount: Int,
) {
    val endSample: Int
        get() = startSample + sampleCount
}

@Composable
internal fun MorseTimelineVisualizer(
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
    val playbackSampleState =
        rememberFlashVisualPlaybackSampleState(
            rawSample = rawCurrentSample.toFloat(),
            isPlaying = isPlaying && !isScrubbing,
            snapWhenNotPlaying = true,
            holdVisualPositionOnPause = true,
            isScrubbing = isScrubbing,
            playbackSpeed = playbackSpeed,
            sampleRateHz = sampleRateHz,
            totalSamples = totalSamples,
        )
    val currentSample =
        playbackSampleState.displayedSample
            .roundToInt()
            .coerceIn(0, totalSamples)
    val windowSamples =
        remember(frameSamples) {
            (frameSamples * 96).coerceAtLeast(frameSamples * 16)
        }
    val window =
        remember(currentSample, windowSamples) {
            resolveMorseTimelineWindow(
                currentSample = currentSample,
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
                    .testTag("morse-timeline-visualizer"),
        ) {
            val drawStartNanos = if (showPerfOverlay) System.nanoTime() else 0L
            drawRoundRect(
                color = backgroundColor,
                size = size,
                cornerRadius = CornerRadius(24f, 24f),
            )

            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 16.dp.toPx()
            val bottomPadding = 16.dp.toPx()
            val innerWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val innerHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val visibleSamples = window.sampleCount.coerceAtLeast(1)
            val centerY = topPadding + innerHeight / 2f

            toneEntries.forEach { entry ->
                val entryStart = entry.startSample
                val entryEnd = entry.startSample + entry.sampleCount
                if (entryEnd < window.startSample || entryStart > window.endSample) {
                    return@forEach
                }
                val visibleStart = entryStart.coerceAtLeast(window.startSample)
                val visibleEnd = entryEnd.coerceAtMost(window.endSample)
                val isDash = entry.sampleCount >= frameSamples * MorseDashUnitThreshold
                val blockHeight =
                    if (isDash) {
                        innerHeight * 0.68f
                    } else {
                        innerHeight * 0.42f
                    }
                val highlightWholeCurrentSegment = currentSample in entryStart until entryEnd
                if (highlightWholeCurrentSegment) {
                    drawMorseTimelineSegmentPart(
                        visibleStart = visibleStart,
                        visibleEnd = visibleEnd,
                        window = window,
                        visibleSamples = visibleSamples,
                        leftPadding = leftPadding,
                        innerWidth = innerWidth,
                        centerY = centerY,
                        blockHeight = blockHeight,
                        color = toneColor.copy(alpha = if (isPlaying) 0.88f else 0.72f),
                    )
                    return@forEach
                }
                drawMorseTimelineSegmentPart(
                    visibleStart = visibleStart,
                    visibleEnd = visibleEnd.coerceAtMost(currentSample),
                    window = window,
                    visibleSamples = visibleSamples,
                    leftPadding = leftPadding,
                    innerWidth = innerWidth,
                    centerY = centerY,
                    blockHeight = blockHeight,
                    color = toneColor.copy(alpha = if (isPlaying) 0.88f else 0.72f),
                )
                drawMorseTimelineSegmentPart(
                    visibleStart = visibleStart.coerceAtLeast(currentSample),
                    visibleEnd = visibleEnd,
                    window = window,
                    visibleSamples = visibleSamples,
                    leftPadding = leftPadding,
                    innerWidth = innerWidth,
                    centerY = centerY,
                    blockHeight = blockHeight,
                    color = inactiveToneColor.copy(alpha = MorseInactiveToneAlpha),
                )
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

@Composable
internal fun MiniVisualPerfOverlay(
    snapshot: MiniVisualPerfSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        contentColor = Color.Black,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text =
                "drawAvg ${"%.2f".format(snapshot.drawAvgMs)}ms  " +
                    "raw ${"%.1f".format(snapshot.rawUpdatesPerSecond)}/s  " +
                    "rawMax ${"%.1f".format(snapshot.rawStepMaxMs)}ms\n" +
                    "smoothMax ${"%.1f".format(snapshot.smoothStepMaxMs)}ms  " +
                    "err ${"%.1f".format(snapshot.visualErrorMs)}ms  " +
                    "window ${"%.1f".format(snapshot.windowStartStepMaxMs)}ms\n" +
                    "sample ${snapshot.rawSample}/${snapshot.currentSample}  sr ${snapshot.sampleRateHz}  " +
                    "window [${snapshot.windowStartSample}, ${snapshot.windowEndSample})",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMorseTimelineSegmentPart(
    visibleStart: Int,
    visibleEnd: Int,
    window: MorseTimelineWindow,
    visibleSamples: Int,
    leftPadding: Float,
    innerWidth: Float,
    centerY: Float,
    blockHeight: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    if (visibleEnd <= visibleStart) {
        return
    }
    val x =
        leftPadding +
            ((visibleStart - window.startSample).toFloat() / visibleSamples.toFloat()) * innerWidth
    val width =
        (((visibleEnd - visibleStart).coerceAtLeast(1)).toFloat() / visibleSamples.toFloat()) *
            innerWidth
    drawRoundRect(
        color = color,
        topLeft = Offset(x, centerY - blockHeight / 2f),
        size = Size(width.coerceAtLeast(2.dp.toPx()), blockHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
}

internal fun resolveMorseTimelineWindow(
    currentSample: Int,
    windowSamples: Int,
): MorseTimelineWindow {
    val safeWindowSamples = windowSamples.coerceAtLeast(1)
    val windowStart = currentSample - (safeWindowSamples * MorseTimelinePlayheadAnchorRatio).toInt()
    return MorseTimelineWindow(
        startSample = windowStart,
        sampleCount = safeWindowSamples,
    )
}

internal fun morseTimelineSampleWidthFraction(
    sampleCount: Int,
    window: MorseTimelineWindow,
): Float = sampleCount.coerceAtLeast(1).toFloat() / window.sampleCount.coerceAtLeast(1).toFloat()

private const val MorseTimelinePlayheadAnchorRatio = 0.40f
private const val MorseDashUnitThreshold = 3
internal const val MorseInactiveToneAlpha = 0.48f
