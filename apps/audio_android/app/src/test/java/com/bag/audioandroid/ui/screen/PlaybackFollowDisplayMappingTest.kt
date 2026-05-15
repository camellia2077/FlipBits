package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.TextFollowCharacterKind
import com.bag.audioandroid.domain.TextFollowCharacterViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFollowDisplayMappingTest {
    @Test
    fun `character display units keep separator entries as blank layout slots`() {
        val units =
            characterDisplayUnits(
                token = "HELLO",
                textCharacters =
                    listOf(
                        TextFollowCharacterViewData(
                            tokenIndex = 0,
                            characterIndexWithinToken = 0,
                            byteIndexWithinToken = 0,
                            byteCount = 1,
                            startSample = 0,
                            sampleCount = 1,
                            kindCode = TextFollowCharacterKind.Visible.wireValue,
                            text = "H",
                        ),
                        TextFollowCharacterViewData(
                            tokenIndex = 0,
                            characterIndexWithinToken = 1,
                            byteIndexWithinToken = 1,
                            byteCount = 1,
                            startSample = 1,
                            sampleCount = 1,
                            kindCode = TextFollowCharacterKind.Space.wireValue,
                            text = " ",
                        ),
                    ),
            )

        assertEquals(2, units.size)
        assertEquals("H", units[0].text)
        assertEquals("\u00A0", units[1].text)
    }

    @Test
    fun `character display units fallback to token text when structured characters are absent`() {
        val units = characterDisplayUnits("中A")

        assertEquals(2, units.size)
        assertEquals("中", units[0].text)
        assertEquals(3, units[0].byteCount)
        assertEquals("A", units[1].text)
        assertTrue(units[1].byteCount > 0)
    }
}
