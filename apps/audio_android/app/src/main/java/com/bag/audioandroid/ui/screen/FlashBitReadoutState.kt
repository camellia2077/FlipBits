package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData

internal data class FlashBitReadoutFrame(
    val currentGroupStartIndex: Int,
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val previousCells: List<FlashBitReadoutCell>,
    val currentCells: List<FlashBitReadoutCell>,
)

internal data class FlashBitReadoutCell(
    val bit: Char?,
    val isCurrent: Boolean,
)

internal data class FlashPulseTapeState(
    val cells: List<FlashPulseCellState>,
    val currentBitProgress: Float,
)

internal data class FlashPulseCellState(
    val bit: Char?,
    val isActive: Boolean,
    val isRevealed: Boolean,
)

internal data class FlashBitReadoutSource(
    val entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    val bitByOffset: Map<Int, Char>,
)

internal fun PayloadFollowViewData.toFlashBitReadoutSource(): FlashBitReadoutSource? {
    if (!followAvailable || binaryGroupTimeline.isEmpty() || binaryTokens.isEmpty()) {
        return null
    }
    return FlashBitReadoutSource(
        entries = binaryGroupTimeline,
        bitByOffset = binaryBitsByOffset(),
    )
}

internal fun flashBitReadoutFrame(
    followData: PayloadFollowViewData,
    sample: Float,
): FlashBitReadoutFrame? {
    val source = followData.toFlashBitReadoutSource() ?: return null
    return flashBitReadoutFrame(source = source, sample = sample)
}

internal fun flashBitReadoutFrame(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashBitReadoutFrame? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val currentGroupStartIndex = (playbackState.revealedBitOffset.coerceAtLeast(0) / FlashBitReadoutGroupSize) * FlashBitReadoutGroupSize
    val previousGroupStartIndex = currentGroupStartIndex - FlashBitReadoutGroupSize
    return FlashBitReadoutFrame(
        currentGroupStartIndex = currentGroupStartIndex,
        currentBitOffset = playbackState.currentBitOffset,
        revealedBitOffset = playbackState.revealedBitOffset,
        previousCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = previousGroupStartIndex,
                revealThroughIndex = previousGroupStartIndex + FlashBitReadoutGroupSize - 1,
                currentBitOffset = null,
            ),
        currentCells =
            buildFlashBitReadoutCells(
                bitByOffset = source.bitByOffset,
                groupStartIndex = currentGroupStartIndex,
                revealThroughIndex = playbackState.revealedBitOffset,
                currentBitOffset = playbackState.currentBitOffset,
            ),
    )
}

internal fun flashPulseTapeState(
    source: FlashBitReadoutSource,
    sample: Float,
): FlashPulseTapeState? {
    if (source.entries.isEmpty() || source.bitByOffset.isEmpty()) {
        return null
    }
    val playbackState = flashTimelinePlaybackState(entries = source.entries, sample = sample)
    val anchorBitOffset = playbackState.currentBitOffset ?: playbackState.revealedBitOffset.takeIf { it >= 0 } ?: return null
    val halfWindow = FlashPulseVisibleCellCount / 2
    return FlashPulseTapeState(
        cells =
            List(FlashPulseVisibleCellCount) { index ->
                val bitOffset = anchorBitOffset + index - halfWindow
                FlashPulseCellState(
                    bit = source.bitByOffset[bitOffset],
                    isActive = playbackState.currentBitOffset == bitOffset,
                    isRevealed = bitOffset <= playbackState.revealedBitOffset,
                )
            },
        currentBitProgress = playbackState.currentBitProgress,
    )
}

private fun buildFlashBitReadoutCells(
    bitByOffset: Map<Int, Char>,
    groupStartIndex: Int,
    revealThroughIndex: Int,
    currentBitOffset: Int?,
): List<FlashBitReadoutCell> =
    List(FlashBitReadoutGroupSize) { slot ->
        val bitOffset = groupStartIndex + slot
        FlashBitReadoutCell(
            bit =
                if (bitOffset >= 0 && bitOffset <= revealThroughIndex) {
                    bitByOffset[bitOffset]
                } else {
                    null
                },
            isCurrent = currentBitOffset == bitOffset,
        )
    }

private data class FlashTimelinePlaybackState(
    val currentBitOffset: Int?,
    val revealedBitOffset: Int,
    val currentBitProgress: Float,
)

private fun flashTimelinePlaybackState(
    entries: List<PayloadFollowBinaryGroupTimelineEntry>,
    sample: Float,
): FlashTimelinePlaybackState {
    var low = 0
    var high = entries.lastIndex
    var previousRevealedBitOffset = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val entry = entries[mid]
        val entryEndSample = entry.startSample + entry.sampleCount
        when {
            sample < entry.startSample -> high = mid - 1
            sample >= entryEndSample -> {
                previousRevealedBitOffset = entry.lastBitOffset
                low = mid + 1
            }
            else -> {
                val bitProgress = entry.bitProgressAtSample(sample)
                val currentBitOffset =
                    entry.bitOffset + bitProgress.toInt().coerceIn(0, entry.bitCount.coerceAtLeast(1) - 1)
                return FlashTimelinePlaybackState(
                    currentBitOffset = currentBitOffset,
                    revealedBitOffset = currentBitOffset,
                    currentBitProgress = bitProgress - bitProgress.toInt(),
                )
            }
        }
    }
    return FlashTimelinePlaybackState(
        currentBitOffset = null,
        revealedBitOffset = previousRevealedBitOffset,
        currentBitProgress = 0f,
    )
}

internal fun FlashBitReadoutSource.currentBitOffsetAtSample(sample: Float): Int? =
    flashTimelinePlaybackState(entries = entries, sample = sample).currentBitOffset

internal fun FlashBitReadoutFrame.currentBitsText(): String = currentCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

internal fun FlashBitReadoutFrame.previousBitsText(): String = previousCells.joinToString(separator = "") { it.bit?.toString() ?: "_" }

private val PayloadFollowBinaryGroupTimelineEntry.lastBitOffset: Int
    get() = bitOffset + bitCount - 1

internal fun PayloadFollowBinaryGroupTimelineEntry.bitProgressAtSample(sample: Float): Float {
    if (bitCount <= 1 || sampleCount <= 0) {
        return 0f
    }
    val progress = ((sample - startSample.toFloat()) / sampleCount.toFloat()).coerceIn(0f, 0.9999f)
    return progress * bitCount.toFloat()
}

private fun PayloadFollowViewData.binaryBitsByOffset(): Map<Int, Char> {
    val bitsByOffset = LinkedHashMap<Int, Char>()
    binaryGroupTimeline.forEach { entry ->
        val bits = binaryTokens.getOrNull(entry.groupIndex).orEmpty().filter { it == '0' || it == '1' }
        repeat(entry.bitCount.coerceAtLeast(0)) { bitIndex ->
            val tokenBitIndex =
                if (bits.length == entry.bitCount) {
                    bitIndex
                } else {
                    (entry.bitOffset + bitIndex).floorMod(bits.length)
                }
            bits.getOrNull(tokenBitIndex)?.let { bit ->
                bitsByOffset[entry.bitOffset + bitIndex] = bit
            }
        }
    }
    return bitsByOffset
}

private fun Int.floorMod(divisor: Int): Int =
    if (divisor <= 0) {
        0
    } else {
        ((this % divisor) + divisor) % divisor
    }

private const val FlashBitReadoutGroupSize = 8
internal const val FlashPulseVisibleCellCount = 13
