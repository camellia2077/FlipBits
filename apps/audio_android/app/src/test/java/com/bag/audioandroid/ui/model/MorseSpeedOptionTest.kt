package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseSpeedOptionTest {
    @Test
    fun `speed presets use WPM ids and Morse dot units`() {
        assertEquals("wpm10", MorseSpeedOption.Wpm10.id)
        assertEquals(5292, MorseSpeedOption.Wpm10.frameSamples(defaultFrameSamples = 2205))
        assertEquals("wpm15", MorseSpeedOption.Wpm15.id)
        assertEquals(3528, MorseSpeedOption.Wpm15.frameSamples(defaultFrameSamples = 2205))
        assertEquals("wpm20", MorseSpeedOption.Wpm20.id)
        assertEquals(2646, MorseSpeedOption.Wpm20.frameSamples(defaultFrameSamples = 2205))
    }

    @Test
    fun `default speed is 15 WPM`() {
        assertEquals(MorseSpeedOption.Wpm15, MorseSpeedOption.default)
        assertEquals(MorseSpeedOption.Wpm15, MorseSpeedOption.fromId(null))
        assertEquals(MorseSpeedOption.Wpm15, MorseSpeedOption.fromId("unknown"))
    }

    @Test
    fun `fromFrameSamples maps to nearest WPM preset`() {
        assertEquals(MorseSpeedOption.Wpm10, MorseSpeedOption.fromFrameSamples(5292))
        assertEquals(MorseSpeedOption.Wpm15, MorseSpeedOption.fromFrameSamples(3528))
        assertEquals(MorseSpeedOption.Wpm20, MorseSpeedOption.fromFrameSamples(2646))
    }
}
