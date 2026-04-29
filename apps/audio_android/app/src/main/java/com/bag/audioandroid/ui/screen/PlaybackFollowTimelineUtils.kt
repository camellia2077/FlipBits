package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry

internal fun annotationByteGroupsForMode(
    mode: PlaybackFollowViewMode,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
): List<String> =
    rawDisplayUnits.map { unit ->
        when (mode) {
            PlaybackFollowViewMode.Hex -> unit.hexText
            PlaybackFollowViewMode.Binary -> unit.binaryText
        }
    }

internal fun activeTextTimelineIndex(
    entries: List<TextFollowTimelineEntry>,
    displayedSamples: Int,
): Int =
    entries.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    }

internal fun activeLineTimelineIndex(
    entries: List<TextFollowLyricLineTimelineEntry>,
    displayedSamples: Int,
): Int =
    entries.indexOfLast { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
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
): Int {
    if (activeTextIndex < 0 || activeByteIndexWithinToken < 0) {
        return -1
    }
    val rawDisplayUnits = rawDisplayUnitsByToken[activeTextIndex].orEmpty()
    val activeByte =
        rawDisplayUnits.firstOrNull { it.byteIndexWithinToken == activeByteIndexWithinToken }
            ?: return -1

    val binaryGroupTimeline = followData.binaryGroupTimeline
    val activeBitGroup = binaryGroupTimeline.firstOrNull { entry ->
        displayedSamples >= entry.startSample &&
            displayedSamples < entry.startSample + entry.sampleCount
    } ?: return -1

    // Use the payload bit offset published by libs instead of deriving it from
    // groupIndex. Flash groups are single bits; coded modes may publish wider
    // groups, but bitOffset remains the stable source of truth for highlighting.
    val byteOffset = activeByte.byteOffset
    val bitIndexInByte = activeBitGroup.bitOffset - (byteOffset * 8)
    return bitIndexInByte.coerceIn(0, 7)
}
