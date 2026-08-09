package com.bag.audioandroid.ui.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val MorseLetterDividerHeight = 34.dp
internal const val MorseLettersPerRow = 6
internal val MorseAnnotationHorizontalPadding = 8.dp
internal val MorseLetterDividerHorizontalPadding = 5.dp
internal val ByteBoundaryDividerThinHeight = 20.dp
internal val ByteBoundaryDividerStrongHeight = 28.dp

internal fun packMorseLetterRows(
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

internal const val BinaryWideRowMinWidthDp = 320f
internal val BinaryAnnotationHorizontalPadding = 4.dp
internal val BinaryAnnotationGroupSpacing = 4.dp
internal val BinaryByteBlockWidth = 68.dp

internal fun annotationLayoutPolicy(
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
