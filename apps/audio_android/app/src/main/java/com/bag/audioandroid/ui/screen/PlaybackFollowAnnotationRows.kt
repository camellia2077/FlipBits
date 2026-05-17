package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

private data class AnnotationLayoutPolicy(
    val byteGroupsPerRow: Int,
    val maxVisibleRows: Int,
)

private data class PlaybackFollowByteAnnotationRenderState(
    val dividerColor: Color,
    val byteGroupsPerRow: Int,
    val visibleWindow: AnnotationWindow,
    val visibleRows: List<List<String>>,
    val boundaryByteIndexes: Map<Int, AnnotationDividerStyle?>,
)

private data class PlaybackFollowMorseAnnotationRenderState(
    val dividerColor: Color,
    val rowGroups: List<List<MorseLetterDisplayGroup>>,
)

@Composable
internal fun PlaybackFollowAnnotationRows(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    annotationByteGroups: List<String>,
    characterDisplayUnits: List<CharacterDisplayUnit>,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int,
    isActiveBitTone: Boolean,
    tokenIndex: Int,
    displayedSamples: Int,
    followData: PayloadFollowViewData?,
    inactiveRawColor: Color,
    focusColor: Color,
    onFocusColor: Color,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        when (annotationMode) {
            PlaybackFollowViewMode.Binary -> {
                val renderState =
                    rememberPlaybackFollowByteAnnotationRenderState(
                        annotationMode = annotationMode,
                        availableWidthDp = maxWidth.value,
                        annotationByteGroups = annotationByteGroups,
                        activeByteIndexWithinToken = activeByteIndexWithinToken,
                        isActive = isActive,
                        rawDisplayUnits = rawDisplayUnits,
                        characterDisplayUnits = characterDisplayUnits,
                    )
                PlaybackBinaryFollowAnnotationContent(
                    token = token,
                    rawDisplayUnits = rawDisplayUnits,
                    tokenIndex = tokenIndex,
                    displayedSamples = displayedSamples,
                    followData = followData,
                    renderState = renderState,
                    inactiveRawColor = inactiveRawColor,
                    focusColor = focusColor,
                    onFocusColor = onFocusColor,
                    isActive = isActive,
                    activeByteIndexWithinToken = activeByteIndexWithinToken,
                    activeBitIndexWithinByte = activeBitIndexWithinByte,
                    isActiveBitTone = isActiveBitTone,
                )
            }

            PlaybackFollowViewMode.Hex -> {
                val renderState =
                    rememberPlaybackFollowByteAnnotationRenderState(
                        annotationMode = annotationMode,
                        availableWidthDp = maxWidth.value,
                        annotationByteGroups = annotationByteGroups,
                        activeByteIndexWithinToken = activeByteIndexWithinToken,
                        isActive = isActive,
                        rawDisplayUnits = rawDisplayUnits,
                        characterDisplayUnits = characterDisplayUnits,
                    )
                PlaybackHexFollowAnnotationContent(
                    renderState = renderState,
                    inactiveRawColor = inactiveRawColor,
                    focusColor = focusColor,
                    onFocusColor = onFocusColor,
                    isActive = isActive,
                    activeByteIndexWithinToken = activeByteIndexWithinToken,
                    activeBitIndexWithinByte = activeBitIndexWithinByte,
                    isActiveBitTone = isActiveBitTone,
                )
            }

            PlaybackFollowViewMode.Morse -> {
                val renderState =
                    rememberPlaybackFollowMorseAnnotationRenderState(
                        token = token,
                        characterDisplayUnits = characterDisplayUnits,
                        rawDisplayUnits = rawDisplayUnits,
                        annotationByteGroups = annotationByteGroups,
                        availableWidth = maxWidth,
                    )
                PlaybackMorseFollowAnnotationContent(
                    renderState = renderState,
                    inactiveRawColor = inactiveRawColor,
                    focusColor = focusColor,
                    onFocusColor = onFocusColor,
                    isActive = isActive,
                    activeByteIndexWithinToken = activeByteIndexWithinToken,
                    activeBitIndexWithinByte = activeBitIndexWithinByte,
                    isActiveBitTone = isActiveBitTone,
                )
            }
        }
    }
}

@Composable
private fun rememberPlaybackFollowByteAnnotationRenderState(
    annotationMode: PlaybackFollowViewMode,
    availableWidthDp: Float,
    annotationByteGroups: List<String>,
    activeByteIndexWithinToken: Int,
    isActive: Boolean,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    characterDisplayUnits: List<CharacterDisplayUnit>,
): PlaybackFollowByteAnnotationRenderState {
    val dividerColor = appThemeVisualTokens().subtleOutlineColor.copy(alpha = 0.7f)
    val layoutPolicy = remember(annotationMode, availableWidthDp) { annotationLayoutPolicy(annotationMode, availableWidthDp) }
    var previousWindowStartIndex by
        rememberSaveable(
            annotationMode,
            annotationByteGroups,
            layoutPolicy.byteGroupsPerRow,
            layoutPolicy.maxVisibleRows,
        ) {
            mutableIntStateOf(0)
        }
    val visibleWindow =
        remember(
            annotationByteGroups,
            layoutPolicy.byteGroupsPerRow,
            layoutPolicy.maxVisibleRows,
            activeByteIndexWithinToken,
            isActive,
            previousWindowStartIndex,
        ) {
            resolveAnnotationWindow(
                annotationByteGroups = annotationByteGroups,
                byteGroupsPerRow = layoutPolicy.byteGroupsPerRow,
                maxVisibleRows = layoutPolicy.maxVisibleRows,
                activeByteIndexWithinToken = activeByteIndexWithinToken,
                centerActiveGroup = isActive,
                previousStartIndex = previousWindowStartIndex,
            )
        }
    SideEffect {
        if (visibleWindow.startIndex != previousWindowStartIndex) {
            previousWindowStartIndex = visibleWindow.startIndex
        }
    }
    val visibleRows =
        remember(visibleWindow.groups, layoutPolicy.byteGroupsPerRow) {
            visibleWindow.groups.chunked(layoutPolicy.byteGroupsPerRow)
        }
    val boundaryByteIndexes =
        remember(rawDisplayUnits, characterDisplayUnits) {
            annotationDividerStylesByBoundary(
                rawDisplayUnits = rawDisplayUnits,
                fallbackCharacterDisplayUnits = characterDisplayUnits,
            )
        }
    return PlaybackFollowByteAnnotationRenderState(
        dividerColor = dividerColor,
        byteGroupsPerRow = layoutPolicy.byteGroupsPerRow,
        visibleWindow = visibleWindow,
        visibleRows = visibleRows,
        boundaryByteIndexes = boundaryByteIndexes,
    )
}

@Composable
private fun rememberPlaybackFollowMorseAnnotationRenderState(
    token: String,
    characterDisplayUnits: List<CharacterDisplayUnit>,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationByteGroups: List<String>,
    availableWidth: Dp,
): PlaybackFollowMorseAnnotationRenderState {
    val dividerColor = appThemeVisualTokens().subtleOutlineColor.copy(alpha = 0.7f)
    val morseLetterGroups =
        remember(token, characterDisplayUnits, rawDisplayUnits, annotationByteGroups) {
            morseLetterDisplayGroups(
                token = token,
                characterDisplayUnits = characterDisplayUnits,
                rawDisplayUnits = rawDisplayUnits,
                annotationByteGroups = annotationByteGroups,
            )
        }
    val availableMorseRowWidth = (availableWidth - MorseAnnotationHorizontalPadding * 2).coerceAtLeast(0.dp)
    val rowGroups =
        remember(morseLetterGroups, availableMorseRowWidth) {
            packMorseLetterRows(
                groups = morseLetterGroups,
                availableWidth = availableMorseRowWidth,
            )
        }
    return PlaybackFollowMorseAnnotationRenderState(
        dividerColor = dividerColor,
        rowGroups = rowGroups,
    )
}

@Composable
private fun PlaybackBinaryFollowAnnotationContent(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    tokenIndex: Int,
    displayedSamples: Int,
    followData: PayloadFollowViewData?,
    renderState: PlaybackFollowByteAnnotationRenderState,
    inactiveRawColor: Color,
    focusColor: Color,
    onFocusColor: Color,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int,
    isActiveBitTone: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BinaryAnnotationHorizontalPadding, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        renderState.visibleRows.forEachIndexed { rowIndex, rowGroups ->
            PlaybackByteGroupRow(horizontalSpacing = BinaryAnnotationGroupSpacing) {
                val isFirstRow = rowIndex == 0
                val isLastRow = rowIndex == renderState.visibleRows.lastIndex
                if (isFirstRow && renderState.visibleWindow.hasLeadingOverflow) {
                    AnnotationOverflowIndicator(color = inactiveRawColor)
                }
                rowGroups.forEachIndexed { groupIndexInRow, group ->
                    val byteIndex = renderState.visibleWindow.byteIndexFor(rowIndex, groupIndexInRow, renderState.byteGroupsPerRow)
                    val isActiveByte = isActive && byteIndex == activeByteIndexWithinToken
                    PlaybackByteBlock(
                        group = group,
                        mode = PlaybackFollowViewMode.Binary,
                        isActive = isActiveByte,
                        isPast = isActive && byteIndex < activeByteIndexWithinToken,
                        activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                        isActiveBitTone = isActiveBitTone,
                        tokenIndex = tokenIndex,
                        tokenText = token,
                        byteIndexWithinToken = byteIndex,
                        globalByteOffset = rawDisplayUnits.getOrNull(byteIndex)?.byteOffset ?: -1,
                        displayedSamples = displayedSamples,
                        followData = followData,
                        focusColor = focusColor,
                        onFocusColor = onFocusColor,
                        inactiveColor = inactiveRawColor,
                        modifier = Modifier.width(BinaryByteBlockWidth),
                    )
                    PlaybackByteBoundaryDivider(
                        style = renderState.boundaryByteIndexes[byteIndex + 1],
                        color = renderState.dividerColor,
                    )
                }
                if (isLastRow && renderState.visibleWindow.hasTrailingOverflow) {
                    AnnotationOverflowIndicator(color = inactiveRawColor)
                }
            }
        }
    }
}

@Composable
private fun PlaybackHexFollowAnnotationContent(
    renderState: PlaybackFollowByteAnnotationRenderState,
    inactiveRawColor: Color,
    focusColor: Color,
    onFocusColor: Color,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int,
    isActiveBitTone: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        renderState.visibleRows.forEachIndexed { rowIndex, rowGroups ->
            PlaybackByteGroupRow {
                val isFirstRow = rowIndex == 0
                val isLastRow = rowIndex == renderState.visibleRows.lastIndex
                if (isFirstRow && renderState.visibleWindow.hasLeadingOverflow) {
                    AnnotationOverflowIndicator(color = inactiveRawColor)
                }
                rowGroups.forEachIndexed { groupIndexInRow, group ->
                    val byteIndex = renderState.visibleWindow.byteIndexFor(rowIndex, groupIndexInRow, renderState.byteGroupsPerRow)
                    val isActiveByte = isActive && byteIndex == activeByteIndexWithinToken
                    PlaybackHexByteBlock(
                        group = group,
                        isActive = isActiveByte,
                        isPast = isActive && byteIndex < activeByteIndexWithinToken,
                        activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                        isActiveBitTone = isActiveBitTone,
                        focusColor = focusColor,
                        onFocusColor = onFocusColor,
                        inactiveColor = inactiveRawColor,
                    )
                    PlaybackByteBoundaryDivider(
                        style = renderState.boundaryByteIndexes[byteIndex + 1],
                        color = renderState.dividerColor,
                    )
                }
                if (isLastRow && renderState.visibleWindow.hasTrailingOverflow) {
                    AnnotationOverflowIndicator(color = inactiveRawColor)
                }
            }
        }
    }
}

@Composable
private fun PlaybackMorseFollowAnnotationContent(
    renderState: PlaybackFollowMorseAnnotationRenderState,
    inactiveRawColor: Color,
    focusColor: Color,
    onFocusColor: Color,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int,
    isActiveBitTone: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MorseAnnotationHorizontalPadding, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        renderState.rowGroups.forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top,
            ) {
                rowGroups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = MorseLetterDividerHorizontalPadding)
                                    .height(MorseLetterDividerHeight)
                                    .width(1.dp)
                                    .background(renderState.dividerColor),
                        )
                    }
                    val isActiveByte =
                        isActive &&
                            activeByteIndexWithinToken >= group.byteStartIndexWithinToken &&
                            activeByteIndexWithinToken < group.byteStartIndexWithinToken + group.byteCount
                    MorseLetterBlock(
                        letter = group.text,
                        morse = group.morse,
                        isActive = isActiveByte,
                        isPast = isActive && group.byteStartIndexWithinToken + group.byteCount - 1 < activeByteIndexWithinToken,
                        activeBitIndex = if (isActiveByte) activeBitIndexWithinByte else -1,
                        isActiveBitTone = isActiveBitTone,
                        focusColor = focusColor,
                        onFocusColor = onFocusColor,
                        inactiveColor = inactiveRawColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackByteGroupRow(
    horizontalSpacing: Dp = 6.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun PlaybackByteBoundaryDivider(
    style: AnnotationDividerStyle?,
    color: Color,
) {
    if (style != null) {
        val height =
            when (style) {
                AnnotationDividerStyle.Thin -> ByteBoundaryDividerThinHeight
                AnnotationDividerStyle.Strong -> ByteBoundaryDividerStrongHeight
            }
        Box(
            modifier =
                Modifier
                    .height(height)
                    .width(1.dp)
                    .background(color),
        )
    }
}

@Composable
private fun AnnotationOverflowIndicator(color: Color) {
    Text(
        text = "...",
        color = color.copy(alpha = 0.72f),
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}

private val MorseLetterDividerHeight = 34.dp
private const val MorseLettersPerRow = 6
private val MorseAnnotationHorizontalPadding = 8.dp
private val MorseLetterDividerHorizontalPadding = 5.dp
private val ByteBoundaryDividerThinHeight = 20.dp
private val ByteBoundaryDividerStrongHeight = 28.dp

private fun packMorseLetterRows(
    groups: List<MorseLetterDisplayGroup>,
    availableWidth: Dp,
): List<List<MorseLetterDisplayGroup>> {
    if (groups.isEmpty()) {
        return emptyList()
    }
    val rows = ArrayList<List<MorseLetterDisplayGroup>>()
    var currentRow = ArrayList<MorseLetterDisplayGroup>()
    var currentRowWidth = 0.dp
    groups.forEach { group ->
        val itemWidth = morseLetterVisualWidthDp(letter = group.text, morse = group.morse)
        val separatorWidth =
            if (currentRow.isEmpty()) {
                0.dp
            } else {
                MorseLetterDividerHorizontalPadding * 2 + 1.dp
            }
        val wouldOverflowWidth =
            currentRow.isNotEmpty() && currentRowWidth + separatorWidth + itemWidth > availableWidth
        if (currentRow.isNotEmpty() && (currentRow.size >= MorseLettersPerRow || wouldOverflowWidth)) {
            rows += currentRow
            currentRow = ArrayList()
            currentRowWidth = 0.dp
        }
        if (currentRow.isNotEmpty()) {
            currentRowWidth += MorseLetterDividerHorizontalPadding * 2 + 1.dp
        }
        currentRow += group
        currentRowWidth += itemWidth
    }
    if (currentRow.isNotEmpty()) {
        rows += currentRow
    }
    return rows
}

internal fun annotationByteGroupsPerRow(
    mode: PlaybackFollowViewMode,
    availableWidthDp: Float,
): Int =
    when (mode) {
        PlaybackFollowViewMode.Hex -> 4

        PlaybackFollowViewMode.Binary ->
            if (availableWidthDp >= BinaryWideRowMinWidthDp) {
                4
            } else {
                3
            }

        PlaybackFollowViewMode.Morse -> 1
    }

private const val BinaryWideRowMinWidthDp = 320f
private val BinaryAnnotationHorizontalPadding = 4.dp
private val BinaryAnnotationGroupSpacing = 4.dp
private val BinaryByteBlockWidth = 68.dp

private fun annotationLayoutPolicy(
    mode: PlaybackFollowViewMode,
    availableWidthDp: Float,
): AnnotationLayoutPolicy =
    AnnotationLayoutPolicy(
        byteGroupsPerRow = annotationByteGroupsPerRow(mode, availableWidthDp),
        maxVisibleRows = annotationMaxVisibleRows(mode),
    )

internal fun annotationMaxVisibleRows(mode: PlaybackFollowViewMode): Int =
    when (mode) {
        PlaybackFollowViewMode.Hex -> 3
        PlaybackFollowViewMode.Binary -> 4
        PlaybackFollowViewMode.Morse -> Int.MAX_VALUE
    }

internal data class AnnotationWindow(
    val startIndex: Int,
    val groups: List<String>,
    val hasLeadingOverflow: Boolean,
    val hasTrailingOverflow: Boolean,
)

internal fun AnnotationWindow.byteIndexFor(
    rowIndex: Int,
    groupIndexInRow: Int,
    byteGroupsPerRow: Int,
): Int = startIndex + rowIndex * byteGroupsPerRow + groupIndexInRow

internal fun resolveAnnotationWindow(
    annotationByteGroups: List<String>,
    byteGroupsPerRow: Int,
    maxVisibleRows: Int,
    activeByteIndexWithinToken: Int,
    centerActiveGroup: Boolean,
    previousStartIndex: Int = 0,
): AnnotationWindow {
    val capacity = (byteGroupsPerRow * maxVisibleRows).coerceAtLeast(1)
    if (annotationByteGroups.size <= capacity) {
        return AnnotationWindow(
            startIndex = 0,
            groups = annotationByteGroups,
            hasLeadingOverflow = false,
            hasTrailingOverflow = false,
        )
    }

    val lastPossibleStart = (annotationByteGroups.size - capacity).coerceAtLeast(0)
    val resolvedStart =
        if (centerActiveGroup && activeByteIndexWithinToken >= 0) {
            resolveWindowStartIndex(
                activeIndex = activeByteIndexWithinToken,
                previousStartIndex = previousStartIndex.coerceIn(0, lastPossibleStart),
                capacity = capacity,
                lastPossibleStart = lastPossibleStart,
            )
        } else {
            0
        }
    val endExclusive = (resolvedStart + capacity).coerceAtMost(annotationByteGroups.size)
    return AnnotationWindow(
        startIndex = resolvedStart,
        groups = annotationByteGroups.subList(resolvedStart, endExclusive),
        hasLeadingOverflow = resolvedStart > 0,
        hasTrailingOverflow = endExclusive < annotationByteGroups.size,
    )
}

internal fun resolveWindowStartIndex(
    activeIndex: Int,
    previousStartIndex: Int,
    capacity: Int,
    lastPossibleStart: Int,
): Int {
    val clampedPreviousStart = previousStartIndex.coerceIn(0, lastPossibleStart)
    if (activeIndex < 0) {
        return clampedPreviousStart
    }
    val comfortMargin = (capacity / 4).coerceAtLeast(2)
    val previousEndExclusive = clampedPreviousStart + capacity
    val safeStart = clampedPreviousStart + comfortMargin
    val safeEndExclusive = previousEndExclusive - comfortMargin
    if (activeIndex in safeStart until safeEndExclusive) {
        return clampedPreviousStart
    }
    val targetOffset = (capacity * 2) / 5
    return (activeIndex - targetOffset).coerceIn(0, lastPossibleStart)
}
