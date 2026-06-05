package com.bag.audioandroid.audio

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MiniCwSpeedAdjustedPcmRendererTest {
    @Test
    fun `mini cw renderer expands all slow playback speeds`() {
        val totalSamples = 4_800
        val context =
            miniContext(
                totalSamples = totalSamples,
                tones = listOf(tone(startSample = 0, sampleCount = totalSamples)),
            )

        listOf(0.1f, 0.25f, 0.5f, 0.75f).forEach { speed ->
            val rendered =
                renderMiniCwSpeedAdjustedPcm(
                    sourceStartSamples = 0,
                    sourceTotalSamples = totalSamples,
                    playbackSpeed = speed,
                    sampleRateHz = SampleRateHz,
                    context = context,
                )

            assertNotNull("speed=$speed should render through mini CW path", rendered)
            assertEquals((totalSamples / speed).toInt(), rendered?.pcm?.size)
            assertEquals(totalSamples, rendered?.timeline?.sourceProgress(rendered.pcm.size))
        }
    }

    @Test
    fun `mini cw slow render keeps carrier near 700 hz`() {
        val totalSamples = 4_800
        val context =
            miniContext(
                totalSamples = totalSamples,
                tones = listOf(tone(startSample = 0, sampleCount = totalSamples)),
            )

        val rendered =
            renderMiniCwSpeedAdjustedPcm(
                sourceStartSamples = 0,
                sourceTotalSamples = totalSamples,
                playbackSpeed = 0.5f,
                sampleRateHz = SampleRateHz,
                context = context,
            )

        val pcm = requireNotNull(rendered).pcm
        val carrierPower = goertzelPower(pcm, SampleRateHz, 700.0)
        val shiftedPower = goertzelPower(pcm, SampleRateHz, 350.0)
        val octavePower = goertzelPower(pcm, SampleRateHz, 1_400.0)
        assertTrue("700Hz carrier should dominate shifted slow-playback pitch", carrierPower > shiftedPower * 20.0)
        assertTrue("700Hz carrier should dominate octave artifact", carrierPower > octavePower * 20.0)
    }

    @Test
    fun `mini cw slow render applies shared raised cosine envelope`() {
        val sourceToneSamples = 480
        val renderedToneSamples = 960
        val context =
            miniContext(
                totalSamples = sourceToneSamples,
                tones = listOf(tone(startSample = 0, sampleCount = sourceToneSamples)),
            )

        val rendered =
            renderMiniCwSpeedAdjustedPcm(
                sourceStartSamples = 0,
                sourceTotalSamples = sourceToneSamples,
                playbackSpeed = 0.5f,
                sampleRateHz = SampleRateHz,
                context = context,
            )

        val pcm = requireNotNull(rendered).pcm
        assertEquals(renderedToneSamples, pcm.size)
        assertEquals(0, pcm.first().toInt())
        assertEquals(0, pcm.last().toInt())
        assertEquals(expectedMiniCwSample(120, renderedToneSamples), pcm[120].toInt())
        assertEquals(expectedMiniCwSample(840, renderedToneSamples), pcm[840].toInt())
        assertTrue(maxAbs(pcm, 1, 120) < maxAbs(pcm, 300, 540))
        assertTrue(maxAbs(pcm, 840, 959) < maxAbs(pcm, 300, 540))
    }

    @Test
    fun `mini cw slow render preserves and stretches silence gaps`() {
        val totalSamples = 1_440
        val context =
            miniContext(
                totalSamples = totalSamples,
                tones =
                    listOf(
                        tone(startSample = 0, sampleCount = 480),
                        tone(startSample = 960, sampleCount = 480),
                    ),
            )

        val rendered =
            renderMiniCwSpeedAdjustedPcm(
                sourceStartSamples = 0,
                sourceTotalSamples = totalSamples,
                playbackSpeed = 0.5f,
                sampleRateHz = SampleRateHz,
                context = context,
            )

        val pcm = requireNotNull(rendered).pcm
        assertEquals(2_880, pcm.size)
        assertTrue(pcm.copyOfRange(0, 960).any { it != 0.toShort() })
        assertTrue(pcm.copyOfRange(960, 1_920).all { it == 0.toShort() })
        assertTrue(pcm.copyOfRange(1_920, 2_880).any { it != 0.toShort() })
    }

    @Test
    fun `mini cw segmented timeline maps rendered playback back to source samples`() {
        val totalSamples = 1_440
        val context =
            miniContext(
                totalSamples = totalSamples,
                tones =
                    listOf(
                        tone(startSample = 0, sampleCount = 480),
                        tone(startSample = 960, sampleCount = 480),
                    ),
            )

        val timeline =
            requireNotNull(
                buildMiniCwSpeedAdjustedTimeline(
                    sourceStartSamples = 0,
                    sourceTotalSamples = totalSamples,
                    playbackSpeed = 0.5f,
                    context = context,
                ),
            )

        assertEquals(0, timeline.sourceProgress(0))
        assertEquals(480, timeline.sourceProgress(960))
        assertEquals(720, timeline.sourceProgress(1_440))
        assertEquals(960, timeline.sourceProgress(1_920))
        assertEquals(1_920, timeline.renderedPositionForSource(960))
        assertEquals(2_880, timeline.renderedPositionForSource(totalSamples))
    }

    @Test
    fun `mini cw chunk render matches full render slice`() {
        val totalSamples = 1_440
        val context =
            miniContext(
                totalSamples = totalSamples,
                tones =
                    listOf(
                        tone(startSample = 0, sampleCount = 480),
                        tone(startSample = 960, sampleCount = 480),
                    ),
            )
        val full =
            requireNotNull(
                renderMiniCwSpeedAdjustedPcm(
                    sourceStartSamples = 0,
                    sourceTotalSamples = totalSamples,
                    playbackSpeed = 0.5f,
                    sampleRateHz = SampleRateHz,
                    context = context,
                ),
            )

        val chunk =
            renderMiniCwSpeedAdjustedPcmChunk(
                sourceStartSamples = 0,
                sourceTotalSamples = totalSamples,
                outputStartSamples = 700,
                outputSampleCount = 900,
                playbackSpeed = 0.5f,
                sampleRateHz = SampleRateHz,
                context = context,
            )

        assertEquals(full.pcm.copyOfRange(700, 1_600).toList(), chunk.toList())
    }

    private fun miniContext(
        totalSamples: Int,
        tones: List<PayloadFollowBinaryGroupTimelineEntry>,
    ): PlaybackRenderContext =
        PlaybackRenderContext(
            isMiniMode = true,
            frameSamples = 480,
            followData =
                PayloadFollowViewData(
                    followAvailable = true,
                    binaryGroupTimeline = tones,
                    totalPcmSampleCount = totalSamples,
                    payloadSampleCount = totalSamples,
                ),
            totalSamples = totalSamples,
        )

    private fun tone(
        startSample: Int,
        sampleCount: Int,
    ): PayloadFollowBinaryGroupTimelineEntry =
        PayloadFollowBinaryGroupTimelineEntry(
            startSample = startSample,
            sampleCount = sampleCount,
            groupIndex = startSample,
            bitOffset = startSample,
            bitCount = 1,
        )

    private fun goertzelPower(
        samples: ShortArray,
        sampleRateHz: Int,
        targetFrequencyHz: Double,
    ): Double {
        val omega = 2.0 * PI * targetFrequencyHz / sampleRateHz.toDouble()
        val coeff = 2.0 * cos(omega)
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        samples.forEach { sample ->
            q0 = coeff * q1 - q2 + sample.toDouble()
            q2 = q1
            q1 = q0
        }
        return q1 * q1 + q2 * q2 - coeff * q1 * q2
    }

    private fun expectedMiniCwSample(
        sampleIndex: Int,
        sampleCount: Int,
    ): Int {
        val gain = miniCwToneEnvelopeGain(sampleIndex, sampleCount)
        val sample =
            MiniCwToneAmplitude *
                gain *
                sin(2.0 * PI * MiniCwToneFrequencyHz * sampleIndex.toDouble() / SampleRateHz.toDouble())
        return sample.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
    }

    private fun miniCwToneEnvelopeGain(
        sampleIndex: Int,
        sampleCount: Int,
    ): Double {
        val rampSamples =
            (SampleRateHz.toDouble() * MiniCwToneEnvelopeSeconds)
                .roundToInt()
                .coerceAtMost(sampleCount / 2)
        var gain = 1.0
        if (sampleIndex < rampSamples) {
            val ratio = sampleIndex.toDouble() / rampSamples.toDouble()
            gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
        }
        val samplesFromEnd = sampleCount - sampleIndex - 1
        if (samplesFromEnd < rampSamples) {
            val ratio = samplesFromEnd.toDouble() / rampSamples.toDouble()
            gain = minOf(gain, 0.5 - 0.5 * cos(PI * ratio))
        }
        return gain
    }

    private fun maxAbs(
        samples: ShortArray,
        startInclusive: Int,
        endExclusive: Int,
    ): Int =
        samples
            .copyOfRange(startInclusive, endExclusive)
            .maxOf { abs(it.toInt()) }

    private companion object {
        const val SampleRateHz = 48_000
        const val MiniCwToneFrequencyHz = 700.0
        const val MiniCwToneAmplitude = 0.75 * 32767.0
        const val MiniCwToneEnvelopeSeconds = 0.005
    }
}
