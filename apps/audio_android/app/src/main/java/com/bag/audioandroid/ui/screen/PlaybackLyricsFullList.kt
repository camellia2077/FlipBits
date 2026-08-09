package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens
import kotlin.math.abs

@Composable
internal fun PlaybackLyricsFullList(
    followData: PayloadFollowViewData,
    displayLineRanges: List<DisplayTokenLineRange>,
    activeLineIndex: Int,
    sampleRateHz: Int,
    onSeekToSample: (Int) -> Unit,
    extraHeight: Dp = 0.dp,
    useFixedHeight: Boolean = true,
    useCenteredSelectionViewport: Boolean = true,
    listState: LazyListState? = null,
    autoSeekOnScrollStop: Boolean = true,
    showSelectionGuideOnlyWhileScrolling: Boolean = false,
    selectionFadeInactiveLines: Boolean = false,
    onSelectedSampleChanged: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(displayLineRanges.size) {
        Log.d("MiniAutomation", "lyricsFullListRendered lineCount=${displayLineRanges.size}")
    }
    val lineStartSamples =
        remember(followData.textTokenTimeline) {
            followData.textTokenTimeline
                .groupBy { it.tokenIndex }
                .mapValues { (_, entries) -> entries.minOfOrNull { it.startSample } }
        }
    val lineItems =
        remember(followData, displayLineRanges, lineStartSamples) {
            displayLineRanges.map { lineRange ->
                val lineText =
                    resolveFullLyricsLineText(
                        followData = followData,
                        lineRange = lineRange,
                    )
                        ?: resolveContinuousViewportLineForRange(
                            followData = followData,
                            tokenRange = lineRange.tokenRange,
                        ).text
                PlaybackLyricLineItem(
                    text = lineText,
                    startSample =
                        lineStartSamples[lineRange.tokenBeginIndex]
                            ?.takeIf { it >= 0 },
                )
            }
        }
    val resolvedListState =
        listState ?: rememberLazyListState(
            initialFirstVisibleItemIndex =
                (
                    if (useCenteredSelectionViewport) {
                        activeLineIndex - 2
                    } else {
                        activeLineIndex
                    }
                ).coerceIn(
                    minimumValue = 0,
                    maximumValue = displayLineRanges.lastIndex.coerceAtLeast(0),
                ),
        )
    val selectedLineIndex by remember(resolvedListState, lineItems, activeLineIndex, useCenteredSelectionViewport) {
        derivedStateOf {
            if (useCenteredSelectionViewport) {
                centeredVisibleLineIndex(
                    listState = resolvedListState,
                    fallbackIndex = activeLineIndex,
                    lineCount = lineItems.size,
                )
            } else {
                activeLineIndex.coerceIn(0, lineItems.lastIndex.coerceAtLeast(0))
            }
        }
    }
    val selectedStartSample = lineItems.getOrNull(selectedLineIndex)?.startSample
    val latestSelectedStartSample by rememberUpdatedState(selectedStartSample)
    val latestOnSeekToSample by rememberUpdatedState(onSeekToSample)
    var userScrollSelectionActive by remember { mutableStateOf(false) }
    var isSelectionGuideVisible by remember { mutableStateOf(!showSelectionGuideOnlyWhileScrolling) }
    LaunchedEffect(selectedStartSample) {
        onSelectedSampleChanged(selectedStartSample)
    }
    LaunchedEffect(resolvedListState, autoSeekOnScrollStop, showSelectionGuideOnlyWhileScrolling, useCenteredSelectionViewport) {
        if (!useCenteredSelectionViewport) {
            isSelectionGuideVisible = false
            return@LaunchedEffect
        }
        snapshotFlow { resolvedListState.isScrollInProgress }
            .collect { isScrollInProgress ->
                isSelectionGuideVisible = !showSelectionGuideOnlyWhileScrolling || isScrollInProgress
                if (isScrollInProgress) {
                    userScrollSelectionActive = true
                } else if (userScrollSelectionActive) {
                    userScrollSelectionActive = false
                    val targetSample = latestSelectedStartSample
                    if (autoSeekOnScrollStop && targetSample != null) {
                        latestOnSeekToSample(targetSample)
                    }
                }
            }
    }
    LaunchedEffect(showSelectionGuideOnlyWhileScrolling) {
        if (!showSelectionGuideOnlyWhileScrolling && useCenteredSelectionViewport) {
            isSelectionGuideVisible = true
        }
    }
    val selectorColor = playbackLyricsSelectionLineColor()
    BoxWithConstraints(
        modifier =
            modifier
                .then(
                    if (useFixedHeight) {
                        Modifier.height(280.dp + extraHeight)
                    } else {
                        Modifier.fillMaxHeight()
                    },
                ).testTag("playback-lyrics-full-list"),
    ) {
        val selectorPadding =
            if (useCenteredSelectionViewport) {
                ((maxHeight - PlaybackLyricsSelectionTargetHeight) / 2).coerceAtLeast(0.dp)
            } else {
                0.dp
            }
        LazyColumn(
            state = resolvedListState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = selectorPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(lineItems) { index, lineItem ->
                val isActive = index == activeLineIndex
                val isSelected = index == selectedLineIndex
                Text(
                    text = lineItem.text,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (lineItem.startSample != null) {
                                    Modifier.clickable { onSeekToSample(lineItem.startSample) }
                                } else {
                                    Modifier
                                },
                            ).testTag(if (isActive) "playback-lyrics-full-line-active" else "playback-lyrics-full-line"),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight =
                                when {
                                    isActive || isSelected -> FontWeight.SemiBold
                                    else -> FontWeight.Normal
                                },
                        ),
                    color =
                        when {
                            selectionFadeInactiveLines && isSelected -> MaterialTheme.colorScheme.onSurface
                            selectionFadeInactiveLines ->
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = PlaybackLyricsSelectionInactiveLineAlpha,
                                )
                            isActive -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        if (useCenteredSelectionViewport && isSelectionGuideVisible) {
            PlaybackLyricsSelectionGuide(
                selectedStartSample = selectedStartSample,
                sampleRateHz = sampleRateHz,
                lineColor = selectorColor,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
            )
        }
    }
}

private data class PlaybackLyricLineItem(
    val text: String,
    val startSample: Int?,
)

private val PlaybackLyricsSelectionTargetHeight = 48.dp
private val PlaybackLyricsSelectionLineWidth = 132.dp

private fun centeredVisibleLineIndex(
    listState: LazyListState,
    fallbackIndex: Int,
    lineCount: Int,
): Int {
    if (lineCount <= 0) {
        return 0
    }
    val layoutInfo = listState.layoutInfo
    val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem =
        layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
            abs((itemInfo.offset + itemInfo.size / 2) - centerOffset)
        }
    return centeredItem
        ?.index
        ?.coerceIn(0, lineCount - 1)
        ?: fallbackIndex.coerceIn(0, lineCount - 1)
}

@Composable
private fun playbackLyricsSelectionLineColor(): Color {
    val visualTokens = appThemeVisualTokens()
    val outlineColor =
        visualTokens.subtleOutlineColor
            .takeIf { it.isSpecified }
            ?: MaterialTheme.colorScheme.outline
    return outlineColor.copy(alpha = PlaybackLyricsSelectionLineAlpha)
}

private const val PlaybackLyricsSelectionLineAlpha = 0.42f
private const val PlaybackLyricsSelectionInactiveLineAlpha = 0.42f

@Composable
private fun PlaybackLyricsSelectionGuide(
    selectedStartSample: Int?,
    sampleRateHz: Int,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val selectedTime =
        formatDurationMillis(
            samplesToMillis(
                samples = selectedStartSample ?: 0,
                sampleRateHz = sampleRateHz,
            ),
        )
    Row(
        modifier =
            modifier
                .height(PlaybackLyricsSelectionTargetHeight)
                .testTag("playback-lyrics-selection-guide"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .width(PlaybackLyricsSelectionLineWidth)
                    .height(1.dp)
                    .background(lineColor)
                    .testTag("playback-lyrics-selection-line"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedTime,
            modifier =
                Modifier
                    .width(48.dp)
                    .testTag("playback-lyrics-selection-time"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun resolveFullLyricsLineText(
    followData: PayloadFollowViewData,
    lineRange: DisplayTokenLineRange,
): String? =
    if (lineRange.coversFullSourceLine) {
        followData.lyricLines.getOrNull(lineRange.sourceLineIndex)
    } else {
        null
    }
