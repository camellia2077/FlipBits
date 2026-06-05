package com.bag.audioandroid.audio

import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class FlashSpeedAdjustedPcmRendererTest {
    @Test
    fun `flash renderer expands all low speeds by inserting group spacing`() {
        val source = flashToneSource()
        val context = flashContext(totalSamples = source.size)

        listOf(0.1f, 0.25f, 0.5f, 0.75f).forEach { speed ->
            val rendered =
                renderFlashSpeedAdjustedPcm(
                    sourcePcm = source,
                    sourceStartSamples = 0,
                    sourceTotalSamples = source.size,
                    playbackSpeed = speed,
                    sampleRateHz = SampleRateHz,
                    context = context,
                )

            assertNotNull("speed=$speed should render through Flash path", rendered)
            assertEquals((source.size / speed).roundToInt(), rendered?.pcm?.size)
            assertEquals(source.size, rendered?.timeline?.sourceProgress(rendered.pcm.size))
        }
    }

    @Test
    fun `flash renderer inserts spacing between adjacent groups`() {
        val source = flashToneSource()
        val context = flashContext(totalSamples = source.size)
        val rendered =
            requireNotNull(
                renderFlashSpeedAdjustedPcm(
                    sourcePcm = source,
                    sourceStartSamples = 0,
                    sourceTotalSamples = source.size,
                    playbackSpeed = 0.5f,
                    sampleRateHz = SampleRateHz,
                    context = context,
                ),
            )

        assertTrue(rendered.pcm.copyOfRange(GroupSamples, GroupSamples * 2).all { it == 0.toShort() })
    }

    @Test
    fun `flash renderer keeps carrier energy above shifted pitch`() {
        val source = flashToneSource()
        val context = flashContext(totalSamples = source.size)

        listOf(0.1f, 0.25f, 0.5f, 0.75f).forEach { speed ->
            val rendered =
                requireNotNull(
                    renderFlashSpeedAdjustedPcm(
                        sourcePcm = source,
                        sourceStartSamples = 0,
                        sourceTotalSamples = source.size,
                        playbackSpeed = speed,
                        sampleRateHz = SampleRateHz,
                        context = context,
                    ),
                )

            val carrierPower = goertzelPower(rendered.pcm, SampleRateHz, CarrierHz)
            val shiftedPower = goertzelPower(rendered.pcm, SampleRateHz, CarrierHz * speed.toDouble())
            assertTrue(
                "Flash carrier should dominate shifted slow-playback pitch at speed=$speed",
                carrierPower > shiftedPower * 20.0,
            )
        }
    }

    @Test
    fun `flash renderer segmented timeline maps rendered spacing back to source`() {
        val source = flashToneSource()
        val context = flashContext(totalSamples = source.size)
        val timeline =
            requireNotNull(
                buildFlashSpeedAdjustedTimeline(
                    sourceStartSamples = 0,
                    sourceTotalSamples = source.size,
                    playbackSpeed = 0.5f,
                    context = context,
                ),
            )

        assertEquals(0, timeline.sourceProgress(0))
        assertEquals(GroupSamples, timeline.sourceProgress(GroupSamples))
        assertEquals(GroupSamples, timeline.sourceProgress(GroupSamples + 100))
        assertEquals(GroupSamples * 2, timeline.renderedPositionForSource(GroupSamples))
        assertEquals(source.size * 2, timeline.renderedPositionForSource(source.size))
    }

    @Test
    fun `flash chunk render matches full render slice`() {
        val source = flashToneSource()
        val context = flashContext(totalSamples = source.size)
        val full =
            requireNotNull(
                renderFlashSpeedAdjustedPcm(
                    sourcePcm = source,
                    sourceStartSamples = 0,
                    sourceTotalSamples = source.size,
                    playbackSpeed = 0.5f,
                    sampleRateHz = SampleRateHz,
                    context = context,
                ),
            )

        val chunk =
            renderFlashSpeedAdjustedPcmChunk(
                sourcePcm = source,
                sourceStartSamples = 0,
                sourceTotalSamples = source.size,
                outputStartSamples = 800,
                outputSampleCount = 900,
                playbackSpeed = 0.5f,
                sampleRateHz = SampleRateHz,
                context = context,
            )

        assertEquals(full.pcm.copyOfRange(800, 1_700).toList(), chunk.toList())
    }

    private fun flashToneSource(): ShortArray =
        ShortArray(GroupSamples * 2) { index ->
            val groupOffset = index % GroupSamples
            val gain = flashTestEnvelope(groupOffset, GroupSamples)
            val sample =
                FlashToneAmplitude *
                    gain *
                    sin(2.0 * PI * CarrierHz * groupOffset.toDouble() / SampleRateHz.toDouble())
            sample.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

    private fun flashContext(totalSamples: Int): PlaybackRenderContext =
        PlaybackRenderContext(
            isFlashMode = true,
            followData =
                PayloadFollowViewData(
                    followAvailable = true,
                    binaryGroupTimeline =
                        listOf(
                            group(startSample = 0),
                            group(startSample = GroupSamples),
                        ),
                    totalPcmSampleCount = totalSamples,
                    payloadSampleCount = totalSamples,
                ),
            totalSamples = totalSamples,
        )

    private fun group(startSample: Int): PayloadFollowBinaryGroupTimelineEntry =
        PayloadFollowBinaryGroupTimelineEntry(
            startSample = startSample,
            sampleCount = GroupSamples,
            groupIndex = startSample / GroupSamples,
            bitOffset = startSample / GroupSamples,
            bitCount = 1,
            carrierFrequencyHz = CarrierHz.toFloat(),
        )

    private fun flashTestEnvelope(
        sampleIndex: Int,
        sampleCount: Int,
    ): Double {
        val rampSamples = 48
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

    private companion object {
        const val SampleRateHz = 48_000
        const val CarrierHz = 600.0
        const val GroupSamples = 480
        const val FlashToneAmplitude = 0.75 * 32767.0
    }
}
