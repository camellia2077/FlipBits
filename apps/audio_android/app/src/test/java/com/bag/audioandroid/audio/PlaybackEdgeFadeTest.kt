package com.bag.audioandroid.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEdgeFadeTest {
    @Test
    fun `playback start fade ramps only the leading edge`() {
        val samples = ShortArray(24) { 10_000 }

        applyPlaybackStartFadeInPlace(
            samples = samples,
            absoluteStartSamples = 0,
            sampleRateHz = 1_000,
        )

        assertEquals(0, samples[0].toInt())
        assertTrue(samples[3].toInt() in 1 until 10_000)
        assertTrue(samples[6] > samples[3])
        assertEquals(10_000, samples[12].toInt())
        assertEquals(10_000, samples[23].toInt())
    }

    @Test
    fun `playback start fade continues across chunk offsets`() {
        val samples = ShortArray(8) { 10_000 }

        applyPlaybackStartFadeInPlace(
            samples = samples,
            absoluteStartSamples = 10,
            sampleRateHz = 1_000,
        )

        assertTrue(samples[0].toInt() in 1 until 10_000)
        assertEquals(10_000, samples[2].toInt())
        assertEquals(10_000, samples[7].toInt())
    }

    @Test
    fun `playback chunk fade leaves source pcm unchanged`() {
        val source = ShortArray(24) { 10_000 }

        val chunk =
            playbackStartFadedChunk(
                source = source,
                sourceOffset = 0,
                sampleCount = source.size,
                sampleRateHz = 1_000,
            )

        assertEquals(0, chunk[0].toInt())
        assertEquals(10_000, source[0].toInt())
        assertEquals(10_000, source[12].toInt())
    }
}
