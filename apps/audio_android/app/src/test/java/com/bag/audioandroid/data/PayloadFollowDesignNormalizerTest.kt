package com.bag.audioandroid.data

import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.text.Charsets.UTF_8

class PayloadFollowDesignNormalizerTest {
    @Test
    fun `normalizer still detaches punctuation for long payloads`() {
        val originalTokens = listOf("WOW!") + List(318) { "a" }
        val normalized =
            PayloadFollowViewData(
                textTokens = originalTokens,
                textRawDisplayUnits = buildRawDisplayUnits(originalTokens),
                lineTokenRanges = listOf(TextFollowLineTokenRangeViewData(0, 0, originalTokens.size)),
                textFollowAvailable = true,
            ).normalizeDesignTokens()

        assertEquals(listOf("WOW", "!"), normalized.textTokens.take(2))
        assertEquals(originalTokens.size + 1, normalized.textTokens.size)
        assertEquals(originalTokens.size + 1, normalized.lineTokenRanges.single().tokenCount)
    }

    @Test
    fun `normalizer splits unicode punctuation into standalone tokens`() {
        val normalized =
            PayloadFollowViewData(
                textTokens = listOf("«Salut!»"),
                textRawDisplayUnits = buildRawDisplayUnits(listOf("«Salut!»")),
                textFollowAvailable = true,
            ).normalizeDesignTokens()

        assertEquals(listOf("«", "Salut", "!", "»"), normalized.textTokens)
        assertTrue(normalized.textRawDisplayUnits.isNotEmpty())
    }
}

private fun buildRawDisplayUnits(tokens: List<String>): List<TextFollowRawDisplayUnitViewData> {
    val units = ArrayList<TextFollowRawDisplayUnitViewData>()
    var globalByteOffset = 0
    var globalSampleIndex = 0
    tokens.forEachIndexed { tokenIndex, tokenText ->
        var byteIndexWithinToken = 0
        var charIndex = 0
        while (charIndex < tokenText.length) {
            val codePoint = tokenText.codePointAt(charIndex)
            val byteCount = String(Character.toChars(codePoint)).toByteArray(UTF_8).size
            repeat(byteCount) {
                units +=
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = tokenIndex,
                        startSample = globalSampleIndex,
                        sampleCount = 1,
                        byteIndexWithinToken = byteIndexWithinToken,
                        byteOffset = globalByteOffset,
                        byteCount = 1,
                        hexText = "00",
                        binaryText = "00000000",
                    )
                byteIndexWithinToken += 1
                globalByteOffset += 1
                globalSampleIndex += 1
            }
            charIndex += Character.charCount(codePoint)
        }
    }
    return units
}
