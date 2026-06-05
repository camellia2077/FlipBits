package com.bag.audioandroid.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedAdjustedPcmRendererTest {
    @Test
    fun `low speed render expands pcm and keeps source timeline mapping`() {
        val source = ShortArray(1_000) { index -> ((index % 64) * 400 - 12_000).toShort() }

        val rendered =
            renderSpeedAdjustedPcm(
                sourcePcm = source,
                sourceStartSamples = 250,
                sourceTotalSamples = 1_250,
                playbackSpeed = 0.25f,
                sampleRateHz = 48_000,
            )

        assertEquals(4_000, rendered.pcm.size)
        assertEquals(250, rendered.timeline.sourceProgress(0))
        assertEquals(750, rendered.timeline.sourceProgress(2_000))
        assertEquals(1_250, rendered.timeline.sourceProgress(rendered.pcm.size))
        assertEquals(2_000, rendered.timeline.renderedPositionForSource(750))
    }

    @Test
    fun `low speed render produces continuous non-empty pcm for extreme slow speed`() {
        val source = ShortArray(256) { index -> if (index % 32 < 16) 12_000 else (-12_000).toShort() }

        val rendered =
            renderSpeedAdjustedPcm(
                sourcePcm = source,
                sourceStartSamples = 0,
                sourceTotalSamples = source.size,
                playbackSpeed = 0.1f,
                sampleRateHz = 48_000,
            )

        assertEquals(2_560, rendered.pcm.size)
        assertTrue(rendered.pcm.any { it != 0.toShort() })
        assertEquals(source.size, rendered.timeline.sourceProgress(rendered.pcm.size))
    }

    @Test
    fun `fast speed render compresses pcm and keeps source timeline mapping`() {
        val source = ShortArray(1_200) { index -> ((index % 80) * 300 - 9_000).toShort() }

        val rendered =
            renderSpeedAdjustedPcm(
                sourcePcm = source,
                sourceStartSamples = 100,
                sourceTotalSamples = 1_300,
                playbackSpeed = 4.0f,
                sampleRateHz = 48_000,
            )

        assertEquals(300, rendered.pcm.size)
        assertEquals(100, rendered.timeline.sourceProgress(0))
        assertEquals(700, rendered.timeline.sourceProgress(150))
        assertEquals(1_300, rendered.timeline.sourceProgress(rendered.pcm.size))
        assertEquals(150, rendered.timeline.renderedPositionForSource(700))
        assertTrue(rendered.pcm.any { it != 0.toShort() })
    }

    @Test
    fun `chunk render matches full render slice`() {
        val source = ShortArray(2_048) { index -> ((index % 96) * 250 - 8_000).toShort() }
        val full =
            renderSpeedAdjustedPcm(
                sourcePcm = source,
                sourceStartSamples = 0,
                sourceTotalSamples = source.size,
                playbackSpeed = 0.25f,
                sampleRateHz = 48_000,
            )

        val chunk =
            renderSpeedAdjustedPcmChunk(
                sourcePcm = source,
                outputStartSamples = 1_000,
                outputSampleCount = 1_500,
                playbackSpeed = 0.25f,
                sampleRateHz = 48_000,
            )

        assertEquals(full.pcm.copyOfRange(1_000, 2_500).toList(), chunk.toList())
    }

    @Test
    fun `long slow render uses streaming threshold`() {
        val sourceSamples = 6_911_334
        val renderedSamples = speedAdjustedRenderedSampleCount(sourceSamples, 0.1f)

        assertTrue(renderedSamples > 69_000_000)
        assertTrue(shouldStreamSpeedAdjustedPcm(sourceSamples, 0.1f))
    }
}
