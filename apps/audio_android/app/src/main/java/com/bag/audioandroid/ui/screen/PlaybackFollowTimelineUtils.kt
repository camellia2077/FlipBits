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
    val activeBitGroup =
        binaryGroupTimeline.firstOrNull { entry ->
            displayedSamples >= entry.startSample &&
                displayedSamples < entry.startSample + entry.sampleCount &&
                entry.bitOffset >= byteBitStart &&
                entry.bitOffset < byteBitEnd
        }
    if (activeBitGroup != null) {
        return ActiveBitPosition(
            bitIndexWithinByte = (activeBitGroup.bitOffset - byteBitStart).coerceIn(0, 7),
            isToneActive = true,
        )
    }

    // During Litany/Collapse silence, keep the last completed bit visible but
    // mark it inactive so completed hex/bin history does not blink again.
    val completedBitGroup =
        binaryGroupTimeline
            .filter { entry ->
                entry.startSample + entry.sampleCount <= displayedSamples &&
                    entry.bitOffset < byteBitEnd &&
                    entry.bitOffset + entry.bitCount > byteBitStart
            }.maxByOrNull { it.startSample + it.sampleCount }
            ?: return ActiveBitPosition.Inactive

    return ActiveBitPosition(
        bitIndexWithinByte =
            (completedBitGroup.bitOffset + completedBitGroup.bitCount - 1 - byteBitStart)
                .coerceIn(0, 7),
        isToneActive = false,
    )
}

internal data class ActiveBitPosition(
    val bitIndexWithinByte: Int,
    val isToneActive: Boolean,
) {
    companion object {
        val Inactive = ActiveBitPosition(bitIndexWithinByte = -1, isToneActive = false)
    }
}
