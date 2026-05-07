package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedOptionTest {
    @Test
    fun `playback speed labels show selected custom speed`() {
        assertEquals("0.25x", PlaybackSpeedOption.format(0.25f))
        assertEquals("0.5x", PlaybackSpeedOption.format(0.5f))
        assertEquals("1.0x", PlaybackSpeedOption.format(1.0f))
        assertEquals("1.3x", PlaybackSpeedOption.format(1.3f))
        assertEquals("3.7x", PlaybackSpeedOption.format(3.7f))
    }
}
