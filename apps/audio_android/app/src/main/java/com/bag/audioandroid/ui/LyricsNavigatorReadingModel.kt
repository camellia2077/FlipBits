package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowCharacterKind

data class LyricsNavigatorReadingModel(
    val text: String,
    val sampleAtOffset: IntArray,
)

internal fun buildLyricsNavigatorReadingModel(followData: PayloadFollowViewData): LyricsNavigatorReadingModel {
    if (followData.textCharacters.isNotEmpty()) {
        val sortedCharacters =
            followData.textCharacters.sortedWith(
                compareBy(
                    com.bag.audioandroid.domain.TextFollowCharacterViewData::tokenIndex,
                    com.bag.audioandroid.domain.TextFollowCharacterViewData::characterIndexWithinToken,
                    com.bag.audioandroid.domain.TextFollowCharacterViewData::byteIndexWithinToken,
                ),
            )
        val textBuilder = StringBuilder()
        val samples = ArrayList<Int>()
        sortedCharacters.forEach { entry ->
            val segmentText =
                when (entry.kind) {
                    TextFollowCharacterKind.Visible,
                    TextFollowCharacterKind.Space,
                    TextFollowCharacterKind.Newline,
                    TextFollowCharacterKind.SeparatorOther,
                    -> entry.text
                }
            if (segmentText.isEmpty()) {
                return@forEach
            }
            textBuilder.append(segmentText)
            repeat(segmentText.length) {
                samples += entry.startSample
            }
        }
        return LyricsNavigatorReadingModel(
            text = textBuilder.toString(),
            sampleAtOffset = samples.toIntArray(),
        )
    }

    if (followData.lyricLines.isNotEmpty()) {
        val lineStartSamples =
            followData.lineTokenRanges.associate { range ->
                range.lineIndex to
                    (
                        followData.textTokenTimeline.firstOrNull { it.tokenIndex == range.tokenBeginIndex }?.startSample
                            ?: -1
                    )
            }
        val textBuilder = StringBuilder()
        val samples = ArrayList<Int>()
        followData.lyricLines.forEachIndexed { index, line ->
            if (index > 0) {
                textBuilder.append('\n')
                samples += -1
            }
            textBuilder.append(line)
            val lineStartSample = lineStartSamples[index] ?: -1
            repeat(line.length) {
                samples += lineStartSample
            }
        }
        return LyricsNavigatorReadingModel(
            text = textBuilder.toString(),
            sampleAtOffset = samples.toIntArray(),
        )
    }

    val fallbackText = followData.textTokens.joinToString(separator = " ")
    return LyricsNavigatorReadingModel(
        text = fallbackText,
        sampleAtOffset = IntArray(fallbackText.length) { -1 },
    )
}

internal fun resolveSeekSampleForReadingLine(
    text: String,
    sampleAtOffset: IntArray,
    lineStart: Int,
    lineEnd: Int,
): Int? {
    if (text.isEmpty() || sampleAtOffset.isEmpty()) {
        return null
    }
    val safeStart = lineStart.coerceIn(0, sampleAtOffset.lastIndex)
    val safeEndExclusive = lineEnd.coerceIn(safeStart + 1, sampleAtOffset.size)
    for (offset in safeStart until safeEndExclusive) {
        val ch = text[offset]
        if (ch == '\n' || ch == '\r') {
            continue
        }
        val sample = sampleAtOffset[offset]
        if (sample >= 0) {
            return sample
        }
    }
    return null
}
