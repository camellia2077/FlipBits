package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.model.morsePatternForChar

internal fun annotationByteGroupsForMode(
    mode: PlaybackFollowViewMode,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<String> =
    rawDisplayUnits.map { unit ->
        when (mode) {
            PlaybackFollowViewMode.Hex -> unit.hexText
            PlaybackFollowViewMode.Binary -> unit.binaryText
            PlaybackFollowViewMode.Morse -> unit.hexText.hexByteToMorsePattern().orEmpty()
        }
    }

private fun String.hexByteToMorsePattern(): String? {
    val value = toIntOrNull(radix = 16) ?: return null
    return morsePatternForChar(value.toChar())
}

internal fun activeTextTimelineIndex(
    entries: List<TextFollowTimelineEntry>,
    displayedSamples: Int,
): Int =
    activeTimelineIndexBySample(
        entries = entries,
        displayedSamples = displayedSamples,
        startSample = TextFollowTimelineEntry::startSample,
        sampleCount = TextFollowTimelineEntry::sampleCount,
        keepPreviousEntryActiveAcrossGaps = true,
    )

internal fun activeLineTimelineIndex(
    entries: List<TextFollowLyricLineTimelineEntry>,
    displayedSamples: Int,
): Int =
    activeTimelineIndexBySample(
        entries = entries,
        displayedSamples = displayedSamples,
        startSample = TextFollowLyricLineTimelineEntry::startSample,
        sampleCount = TextFollowLyricLineTimelineEntry::sampleCount,
        keepPreviousEntryActiveAcrossGaps = true,
    )

internal fun <T> activeTimelineIndexBySample(
    entries: List<T>,
    displayedSamples: Int,
    startSample: (T) -> Int,
    sampleCount: (T) -> Int,
    keepPreviousEntryActiveAcrossGaps: Boolean = false,
): Int {
    var low = 0
    var high = entries.lastIndex
    var previousEntryIndex = -1
    while (low <= high) {
        val mid = (low + high).ushr(1)
        val entry = entries[mid]
        val start = startSample(entry)
        val end = start + sampleCount(entry)
        when {
            displayedSamples < start -> high = mid - 1
            displayedSamples >= end -> {
                previousEntryIndex = mid
                low = mid + 1
            }
            else -> return mid
        }
    }
    return if (keepPreviousEntryActiveAcrossGaps) previousEntryIndex else -1
}

internal fun activeByteIndexWithinToken(
    activeTextIndex: Int,
    displayedSamples: Int,
    rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
): Int {
    if (activeTextIndex < 0) {
        return -1
    }
    val rawDisplayUnits = rawDisplayUnitsByToken[activeTextIndex].orEmpty()
    val activeByte =
        rawDisplayUnits.firstOrNull { entry ->
            displayedSamples >= entry.startSample &&
                displayedSamples < entry.startSample + entry.sampleCount
        } ?: return -1
    return activeByte.byteIndexWithinToken
}

internal fun activeBitIndexWithinByte(
    activeTextIndex: Int,
    activeByteIndexWithinToken: Int,
    displayedSamples: Int,
    followData: com.bag.audioandroid.domain.PayloadFollowViewData,
    rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
): Int =
    activeBitPositionWithinByte(
        activeTextIndex = activeTextIndex,
        activeByteIndexWithinToken = activeByteIndexWithinToken,
        displayedSamples = displayedSamples,
        followData = followData,
        rawDisplayUnitsByToken = rawDisplayUnitsByToken,
    ).bitIndexWithinByte

internal fun activeBitPositionWithinByte(
    activeTextIndex: Int,
    activeByteIndexWithinToken: Int,
    displayedSamples: Int,
    followData: com.bag.audioandroid.domain.PayloadFollowViewData,
    rawDisplayUnitsByToken: Map<Int, List<TextFollowRawDisplayUnitViewData>>,
): ActiveBitPosition {
    if (activeTextIndex < 0 || activeByteIndexWithinToken < 0) {
        return ActiveBitPosition.Inactive
    }
    val rawDisplayUnits = rawDisplayUnitsByToken[activeTextIndex].orEmpty()
    val activeByte =
        rawDisplayUnits.firstOrNull { it.byteIndexWithinToken == activeByteIndexWithinToken }
            ?: return ActiveBitPosition.Inactive

    val binaryGroupTimeline = followData.binaryGroupTimeline
    val byteBitStart = activeByte.byteOffset * 8
    val byteBitEnd = byteBitStart + activeByte.byteCount * 8
    val byteBitGroups =
        binaryGroupTimeline.filter { entry ->
            entry.bitOffset < byteBitEnd &&
                entry.bitOffset + entry.bitCount > byteBitStart
        }
    val activeBitGroup =
        byteBitGroups.firstOrNull { entry ->
            displayedSamples >= entry.startSample &&
                displayedSamples < entry.startSample + entry.sampleCount
        }
    if (activeBitGroup != null) {
        val bitIndexWithinByte = (activeBitGroup.bitOffset - byteBitStart).coerceIn(0, 7)
        return ActiveBitPosition(
            bitIndexWithinByte = bitIndexWithinByte,
            bitCountWithinByte = activeBitGroup.bitCount.coerceIn(1, 8 - bitIndexWithinByte),
            isToneActive = true,
        )
    }

    // During Litany/Collapse silence, keep the last completed bit visible but
    // mark it inactive so completed hex/bin history does not blink again.
    val completedBitGroup =
        byteBitGroups
            .filter { entry ->
                entry.startSample + entry.sampleCount <= displayedSamples
            }.maxByOrNull { it.startSample + it.sampleCount }
    if (completedBitGroup == null && byteBitGroups.isEmpty()) {
        return activeBitPositionFromByteProgress(
            activeByte = activeByte,
            displayedSamples = displayedSamples,
        )
    }
    if (completedBitGroup == null) {
        return ActiveBitPosition.Inactive
    }

    val completedBitIndexWithinByte =
        (completedBitGroup.bitOffset + completedBitGroup.bitCount - 1 - byteBitStart)
            .coerceIn(0, 7)
    return ActiveBitPosition(
        bitIndexWithinByte = completedBitIndexWithinByte,
        bitCountWithinByte = 1,
        isToneActive = false,
    )
}

private fun activeBitPositionFromByteProgress(
    activeByte: TextFollowRawDisplayUnitViewData,
    displayedSamples: Int,
): ActiveBitPosition {
    if (activeByte.sampleCount <= 0 ||
        displayedSamples < activeByte.startSample ||
        displayedSamples >= activeByte.startSample + activeByte.sampleCount
    ) {
        return ActiveBitPosition.Inactive
    }
    val bitCount = activeByte.byteCount.coerceAtLeast(1) * 8
    val progress =
        ((displayedSamples - activeByte.startSample).toFloat() / activeByte.sampleCount.toFloat())
            .coerceIn(0f, 0.9999f)
    return ActiveBitPosition(
        bitIndexWithinByte = (progress * bitCount).toInt().coerceIn(0, 7),
        bitCountWithinByte = 1,
        isToneActive = true,
    )
}

internal data class ActiveBitPosition(
    val bitIndexWithinByte: Int,
    val bitCountWithinByte: Int,
    val isToneActive: Boolean,
) {
    companion object {
        val Inactive = ActiveBitPosition(bitIndexWithinByte = -1, bitCountWithinByte = 0, isToneActive = false)
    }
}
