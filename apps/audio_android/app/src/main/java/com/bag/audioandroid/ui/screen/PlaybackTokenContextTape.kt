package com.bag.audioandroid.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry

private const val TokenTapeAnimationDurationMs = 180
private const val TokenTapeAnchorRatio = 0.72f
private val TokenTapeHorizontalPadding = 16.dp
private val TokenTapeVerticalPadding = 6.dp
private val TokenTapeFadeWidth = 28.dp

internal data class ContinuousViewportTokenSegment(
    val tokenIndex: Int,
    val start: Int,
    val endExclusive: Int,
)

internal data class ContinuousViewportLine(
    val text: String,
    val tokenSegments: List<ContinuousViewportTokenSegment>,
)

internal data class TokenPixelBounds(
    val startPx: Float,
    val endPx: Float,
) {
    val widthPx: Float
        get() = (endPx - startPx).coerceAtLeast(0f)
}

@Composable
internal fun PlaybackTokenContextTape(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    modifier: Modifier = Modifier,
) {
    if (!followData.textFollowAvailable || followData.textTokens.isEmpty()) {
        return
    }

    val activeTimelineIndex =
        remember(followData.textTokenTimeline, displayedSamples) {
            followActiveTextTimelineIndex(followData, displayedSamples)
        }
    val activeTimelineEntry = followData.textTokenTimeline.getOrNull(activeTimelineIndex)
    val activeTokenIndex = activeTimelineEntry?.tokenIndex ?: -1
    val lineModel =
        remember(
            followData.textTokens,
            followData.lineTokenRanges,
            followData.lyricLineFollowAvailable,
            activeTokenIndex,
        ) {
            resolveContinuousViewportLine(
                followData = followData,
                activeTokenIndex = activeTokenIndex,
            )
        } ?: return

    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val activeColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousActiveColor",
    )
    val historyColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousHistoryColor",
    )
    val futureColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
        animationSpec = tween(durationMillis = TokenTapeAnimationDurationMs, easing = FastOutSlowInEasing),
        label = "playbackTokenContinuousFutureColor",
    )
    val tokenPixelBounds = remember(lineModel) { mutableStateMapOf<Int, TokenPixelBounds>() }
    var lineTextWidthPx by remember(lineModel) { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds()
                .testTag("playback-token-context-tape"),
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val horizontalPaddingPx = with(density) { TokenTapeHorizontalPadding.toPx() }
        val fadeWidthPx = with(density) { TokenTapeFadeWidth.toPx() }
        val visibleViewportWidthPx =
            (viewportWidthPx - 2f * horizontalPaddingPx - 2f * fadeWidthPx).coerceAtLeast(1f)
        val activeSegment = lineModel.tokenSegments.firstOrNull { it.tokenIndex == activeTokenIndex }
        val activeBounds = activeSegment?.let { tokenPixelBounds[it.tokenIndex] }
        val targetTranslationPx =
            targetContinuousViewportTranslationPx(
                displayedSamples = displayedSamples,
                activeTimelineEntry = activeTimelineEntry,
                activeSegment = activeSegment,
                activeBounds = activeBounds,
                viewportWidthPx = viewportWidthPx,
                visibleViewportWidthPx = visibleViewportWidthPx,
                contentWidthPx = lineTextWidthPx + horizontalPaddingPx * 2f,
                horizontalPaddingPx = horizontalPaddingPx,
                fadeWidthPx = fadeWidthPx,
            )
        val shouldSweepWithinActiveToken =
            shouldSweepContinuousSegment(
                activeTimelineEntry = activeTimelineEntry,
                activeBounds = activeBounds,
                visibleViewportWidthPx = visibleViewportWidthPx,
            )
        val animatedTranslationPx by animateFloatAsState(
            targetValue = targetTranslationPx,
            animationSpec =
                tween(
                    durationMillis = TokenTapeAnimationDurationMs,
                    easing = FastOutSlowInEasing,
                ),
            label = "playbackTokenContinuousTranslation",
        )
        val resolvedTranslationPx =
            if (shouldSweepWithinActiveToken) {
                targetTranslationPx
            } else {
                animatedTranslationPx
            }

        Text(
            text =
                buildContinuousViewportAnnotatedString(
                    lineModel = lineModel,
                    activeTokenIndex = activeTokenIndex,
                    activeColor = activeColor,
                    historyColor = historyColor,
                    futureColor = futureColor,
                ),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult ->
                lineTextWidthPx = layoutResult.size.width.toFloat()
                updateTokenPixelBounds(
                    layoutResult = layoutResult,
                    lineModel = lineModel,
                    tokenPixelBounds = tokenPixelBounds,
                )
            },
            modifier =
                Modifier
                    .graphicsLayer { translationX = resolvedTranslationPx }
                    .padding(horizontal = TokenTapeHorizontalPadding, vertical = TokenTapeVerticalPadding)
                    .testTag("playback-token-context-active"),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .width(TokenTapeFadeWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f)),
                        ),
                    )
                    .testTag("playback-token-context-fade-left"),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(TokenTapeFadeWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(surfaceColor.copy(alpha = 0f), surfaceColor),
                        ),
                    )
                    .testTag("playback-token-context-fade-right"),
        )
    }
}

internal fun buildContinuousViewportAnnotatedString(
    lineModel: ContinuousViewportLine,
    activeTokenIndex: Int,
    activeColor: androidx.compose.ui.graphics.Color,
    historyColor: androidx.compose.ui.graphics.Color,
    futureColor: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    var cursor = 0
    lineModel.tokenSegments.forEach { segment ->
        if (cursor < segment.start) {
            append(lineModel.text.substring(cursor, segment.start))
        }
        val tokenColor =
            when {
                segment.tokenIndex == activeTokenIndex -> activeColor
                activeTokenIndex >= 0 && segment.tokenIndex < activeTokenIndex -> historyColor
                else -> futureColor
            }
        withStyle(
            SpanStyle(
                color = tokenColor,
                fontWeight = if (segment.tokenIndex == activeTokenIndex) FontWeight.SemiBold else FontWeight.Medium,
            ),
        ) {
            append(lineModel.text.substring(segment.start, segment.endExclusive))
        }
        cursor = segment.endExclusive
    }
    if (cursor < lineModel.text.length) {
        append(lineModel.text.substring(cursor))
    }
}

internal fun updateTokenPixelBounds(
    layoutResult: TextLayoutResult,
    lineModel: ContinuousViewportLine,
    tokenPixelBounds: MutableMap<Int, TokenPixelBounds>,
) {
    lineModel.tokenSegments.forEach { segment ->
        if (segment.endExclusive <= segment.start || segment.start >= layoutResult.layoutInput.text.text.length) {
            tokenPixelBounds[segment.tokenIndex] = TokenPixelBounds(0f, 0f)
        } else {
            val safeStart = segment.start.coerceAtMost(layoutResult.layoutInput.text.text.lastIndex)
            val safeEndInclusive = (segment.endExclusive - 1).coerceAtMost(layoutResult.layoutInput.text.text.lastIndex)
            val startBox = layoutResult.getBoundingBox(safeStart)
            val endBox = layoutResult.getBoundingBox(safeEndInclusive)
            tokenPixelBounds[segment.tokenIndex] = TokenPixelBounds(startPx = startBox.left, endPx = endBox.right)
        }
    }
}

internal fun followActiveTextTimelineIndex(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
): Int =
    followData.textTokenTimeline.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

internal fun resolveContinuousViewportLine(
    followData: PayloadFollowViewData,
    activeTokenIndex: Int,
): ContinuousViewportLine? {
    if (followData.textTokens.isEmpty()) {
        return null
    }
    val tokenRange =
        resolveActiveTokenLineRange(
            lineTokenRanges = followData.lineTokenRanges,
            lyricLineFollowAvailable = followData.lyricLineFollowAvailable,
            activeTokenIndex = activeTokenIndex,
            tokenCount = followData.textTokens.size,
        ) ?: (0 until followData.textTokens.size)

    val textBuilder = StringBuilder()
    val tokenSegments = ArrayList<ContinuousViewportTokenSegment>()
    tokenRange.forEachIndexed { indexInLine, tokenIndex ->
        if (indexInLine > 0) {
            textBuilder.append(' ')
        }
        val start = textBuilder.length
        textBuilder.append(followData.textTokens[tokenIndex])
        val endExclusive = textBuilder.length
        tokenSegments +=
            ContinuousViewportTokenSegment(
                tokenIndex = tokenIndex,
                start = start,
                endExclusive = endExclusive,
            )
    }
    return ContinuousViewportLine(
        text = textBuilder.toString(),
        tokenSegments = tokenSegments,
    )
}

internal fun resolveActiveTokenLineRange(
    lineTokenRanges: List<TextFollowLineTokenRangeViewData>,
    lyricLineFollowAvailable: Boolean,
    activeTokenIndex: Int,
    tokenCount: Int,
): IntRange? {
    if (!lyricLineFollowAvailable || activeTokenIndex < 0 || tokenCount <= 0) {
        return null
    }
    val activeLineRange =
        lineTokenRanges.firstOrNull { lineRange ->
            activeTokenIndex >= lineRange.tokenBeginIndex &&
                activeTokenIndex < lineRange.tokenBeginIndex + lineRange.tokenCount
        } ?: return null
    val start = activeLineRange.tokenBeginIndex.coerceIn(0, tokenCount - 1)
    val endInclusive = (activeLineRange.tokenBeginIndex + activeLineRange.tokenCount - 1).coerceIn(start, tokenCount - 1)
    return start..endInclusive
}

internal fun shouldSweepContinuousSegment(
    activeTimelineEntry: TextFollowTimelineEntry?,
    activeBounds: TokenPixelBounds?,
    visibleViewportWidthPx: Float,
): Boolean {
    if (activeTimelineEntry == null || activeTimelineEntry.sampleCount <= 0 || activeBounds == null) {
        return false
    }
    return activeBounds.widthPx > visibleViewportWidthPx
}

internal fun targetContinuousViewportTranslationPx(
    displayedSamples: Int,
    activeTimelineEntry: TextFollowTimelineEntry?,
    activeSegment: ContinuousViewportTokenSegment?,
    activeBounds: TokenPixelBounds?,
    viewportWidthPx: Float,
    visibleViewportWidthPx: Float,
    contentWidthPx: Float,
    horizontalPaddingPx: Float,
    fadeWidthPx: Float,
): Float {
    if (activeSegment == null || activeBounds == null || contentWidthPx <= viewportWidthPx) {
        return 0f
    }
    val visibleViewportStartPx = horizontalPaddingPx + fadeWidthPx
    val minTranslation = viewportWidthPx - contentWidthPx

    if (activeTimelineEntry == null || activeTimelineEntry.sampleCount <= 0 || activeBounds.widthPx <= visibleViewportWidthPx) {
        val tokenCenter = activeBounds.startPx + activeBounds.widthPx / 2f
        val anchorX = visibleViewportStartPx + visibleViewportWidthPx * TokenTapeAnchorRatio
        return (anchorX - tokenCenter).coerceIn(minTranslation, 0f)
    }

    val progress =
        ((displayedSamples - activeTimelineEntry.startSample).toFloat() / activeTimelineEntry.sampleCount.toFloat())
            .coerceIn(0f, 1f)
    val tokenSweepRangePx = (activeBounds.widthPx - visibleViewportWidthPx).coerceAtLeast(0f)
    val desiredVisibleStartInToken = tokenSweepRangePx * progress
    val targetVisibleStartInText = activeBounds.startPx + desiredVisibleStartInToken
    return (visibleViewportStartPx - targetVisibleStartInText).coerceIn(minTranslation, 0f)
}
