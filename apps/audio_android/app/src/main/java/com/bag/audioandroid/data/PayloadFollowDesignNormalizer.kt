package com.bag.audioandroid.data

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import kotlin.text.Charsets.UTF_8

internal fun PayloadFollowViewData.normalizeDesignTokens(): PayloadFollowViewData {
    if (!textFollowAvailable || textTokens.isEmpty() || textRawDisplayUnits.isEmpty()) {
        return this
    }

    // Design intent:
    // Keep text-word grouping for readability, but always detach punctuation into its own token
    // so every visible character can own an exact HEX/BIN span and playback highlight. This is
    // intentionally stricter than natural-language tokenization because the Lyrics view is a
    // text-to-encoding inspection surface, not just a prose renderer. We keep this pass on for
    // every payload size so longer texts do not silently fall back to a different tokenization
    // rule than short texts.
    val rawUnitsByToken =
        textRawDisplayUnits
            .groupBy(TextFollowRawDisplayUnitViewData::tokenIndex)
            .mapValues { (_, units) -> units.sortedBy(TextFollowRawDisplayUnitViewData::byteIndexWithinToken) }

    val splitPlans = ArrayList<TokenSplitPlan>(textTokens.size)
    textTokens.forEachIndexed { tokenIndex, tokenText ->
        val tokenUnits = rawUnitsByToken[tokenIndex].orEmpty()
        splitTokenPlan(tokenIndex, tokenText, tokenUnits)?.let(splitPlans::add) ?: return this
    }

    if (splitPlans.all { it.parts.size == 1 && it.parts.first().text == textTokens[it.originalTokenIndex] }) {
        return this
    }

    val tokenIndexMapping = IntArray(textTokens.size)
    val tokenCountMapping = IntArray(textTokens.size)
    val normalizedTokens = ArrayList<String>()
    val normalizedTimeline = ArrayList<TextFollowTimelineEntry>()
    val normalizedRawSegments = ArrayList<TextFollowRawSegmentViewData>()
    val normalizedRawDisplayUnits = ArrayList<TextFollowRawDisplayUnitViewData>()

    splitPlans.forEach { plan ->
        tokenIndexMapping[plan.originalTokenIndex] = normalizedTokens.size
        tokenCountMapping[plan.originalTokenIndex] = plan.parts.size
        plan.parts.forEach { part ->
            val newTokenIndex = normalizedTokens.size
            normalizedTokens += part.text
            normalizedTimeline +=
                TextFollowTimelineEntry(
                    startSample = part.startSample,
                    sampleCount = part.sampleCount,
                    tokenIndex = newTokenIndex,
                )
            normalizedRawSegments +=
                TextFollowRawSegmentViewData(
                    tokenIndex = newTokenIndex,
                    startSample = part.startSample,
                    sampleCount = part.sampleCount,
                    byteOffset = part.byteOffset,
                    byteCount = part.byteCount,
                    hexText = part.hexText,
                    binaryText = part.binaryText,
                )
            normalizedRawDisplayUnits +=
                part.units.map { unit ->
                    unit.source.copy(
                        tokenIndex = newTokenIndex,
                        byteIndexWithinToken = unit.relativeByteIndexWithinPart,
                    )
                }
        }
    }

    val normalizedLineRanges =
        lineTokenRanges.mapNotNull { lineRange ->
            val originalStart = lineRange.tokenBeginIndex
            val originalEndExclusive = originalStart + lineRange.tokenCount
            if (originalStart !in textTokens.indices || originalEndExclusive > textTokens.size) {
                null
            } else {
                val newTokenBeginIndex = tokenIndexMapping[originalStart]
                val newTokenCount =
                    (originalStart until originalEndExclusive).sumOf { originalTokenIndex ->
                        tokenCountMapping[originalTokenIndex]
                    }
                TextFollowLineTokenRangeViewData(
                    lineIndex = lineRange.lineIndex,
                    tokenBeginIndex = newTokenBeginIndex,
                    tokenCount = newTokenCount,
                )
            }
        }

    return copy(
        textTokens = normalizedTokens,
        textTokenTimeline = normalizedTimeline,
        textRawSegments = normalizedRawSegments,
        textRawDisplayUnits = normalizedRawDisplayUnits,
        lineTokenRanges = normalizedLineRanges,
    )
}

private fun splitTokenPlan(
    originalTokenIndex: Int,
    tokenText: String,
    tokenUnits: List<TextFollowRawDisplayUnitViewData>,
): TokenSplitPlan? {
    val characterParts = buildCharacterParts(tokenText)
    if (characterParts.isEmpty()) {
        return TokenSplitPlan(
            originalTokenIndex = originalTokenIndex,
            parts = listOf(TokenPartPlan(tokenText, tokenUnits.map(::toWholeTokenPartDisplayUnit))),
        )
    }

    var consumedUnitIndex = 0
    val partPlans = ArrayList<TokenPartPlan>(characterParts.size)
    characterParts.forEach { characterPart ->
        var consumedBytesForPart = 0
        val unitsForPart = ArrayList<PartDisplayUnit>()
        while (consumedUnitIndex < tokenUnits.size && consumedBytesForPart < characterPart.byteCount) {
            val sourceUnit = tokenUnits[consumedUnitIndex]
            unitsForPart +=
                PartDisplayUnit(
                    source = sourceUnit,
                    relativeByteIndexWithinPart = consumedBytesForPart,
                )
            consumedBytesForPart += sourceUnit.byteCount
            consumedUnitIndex += 1
        }
        if (consumedBytesForPart != characterPart.byteCount) {
            return null
        }
        partPlans += TokenPartPlan(characterPart.text, unitsForPart)
    }

    if (consumedUnitIndex != tokenUnits.size) {
        return null
    }

    return TokenSplitPlan(
        originalTokenIndex = originalTokenIndex,
        parts = partPlans,
    )
}

private fun buildCharacterParts(tokenText: String): List<CharacterPart> {
    if (tokenText.isEmpty()) {
        return emptyList()
    }

    val mergedParts = ArrayList<CharacterPart>()
    var index = 0
    while (index < tokenText.length) {
        val codePoint = tokenText.codePointAt(index)
        val codePointText = String(Character.toChars(codePoint))
        val codePointByteCount = codePointText.toByteArray(UTF_8).size
        val isPunctuation = isPunctuationCodePoint(codePoint)
        val lastPart = mergedParts.lastOrNull()
        if (!isPunctuation && lastPart != null && !lastPart.isPunctuation) {
            mergedParts[mergedParts.lastIndex] =
                lastPart.copy(
                    text = lastPart.text + codePointText,
                    byteCount = lastPart.byteCount + codePointByteCount,
                )
        } else {
            mergedParts +=
                CharacterPart(
                    text = codePointText,
                    byteCount = codePointByteCount,
                    isPunctuation = isPunctuation,
                )
        }
        index += Character.charCount(codePoint)
    }
    return mergedParts
}

private fun isPunctuationCodePoint(codePoint: Int): Boolean =
    when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        -> true

        else -> false
    }

private data class CharacterPart(
    val text: String,
    val byteCount: Int,
    val isPunctuation: Boolean,
)

private data class PartDisplayUnit(
    val source: TextFollowRawDisplayUnitViewData,
    val relativeByteIndexWithinPart: Int,
)

private fun toWholeTokenPartDisplayUnit(unit: TextFollowRawDisplayUnitViewData): PartDisplayUnit =
    PartDisplayUnit(
        source = unit,
        relativeByteIndexWithinPart = unit.byteIndexWithinToken,
    )

private data class TokenPartPlan(
    val text: String,
    val units: List<PartDisplayUnit>,
) {
    val startSample: Int
        get() = units.firstOrNull()?.source?.startSample ?: 0

    val sampleCount: Int
        get() = units.sumOf { it.source.sampleCount }

    val byteOffset: Int
        get() = units.firstOrNull()?.source?.byteOffset ?: 0

    val byteCount: Int
        get() = units.sumOf { it.source.byteCount }

    val hexText: String
        get() = units.joinToString(separator = " ") { it.source.hexText }

    val binaryText: String
        get() = units.joinToString(separator = " ") { it.source.binaryText }
}

private data class TokenSplitPlan(
    val originalTokenIndex: Int,
    val parts: List<TokenPartPlan>,
)
