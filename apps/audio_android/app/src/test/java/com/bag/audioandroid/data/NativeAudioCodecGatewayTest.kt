package com.bag.audioandroid.data

import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeOperationState
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAudioCodecGatewayTest {
    @Test
    fun `poll snapshot parses state phase progress work units and terminal code`() {
        val snapshot =
            doubleArrayOf(
                EncodeOperationState.Running.nativeValue.toDouble(),
                AudioEncodePhase.Postprocessing.nativeValue.toDouble(),
                0.625,
                0.5,
                12.0,
                24.0,
                4.0,
                8.0,
                BagApiCodes.ERROR_NOT_READY.toDouble(),
                1024.0,
                256.0,
                3.0,
                1.0,
            ).toEncodeOperationSnapshot()

        assertEquals(EncodeOperationState.Running, snapshot.state)
        assertEquals(AudioEncodePhase.Postprocessing, snapshot.phase)
        assertEquals(0.625f, snapshot.overallProgress0To1)
        assertEquals(0.5f, snapshot.phaseProgress0To1)
        assertEquals(12L, snapshot.completedWorkUnits)
        assertEquals(24L, snapshot.totalWorkUnits)
        assertEquals(4L, snapshot.phaseCompletedWorkUnits)
        assertEquals(8L, snapshot.phaseTotalWorkUnits)
        assertEquals(BagApiCodes.ERROR_NOT_READY, snapshot.terminalCode)
        assertEquals(1024L, snapshot.estimatedPcmSampleCount)
        assertEquals(256L, snapshot.payloadByteCount)
        assertEquals(3L, snapshot.segmentCount)
        assertEquals(1L, snapshot.currentSegmentIndex)
    }

    @Test
    fun `poll work plan parses totals and counts`() {
        val workPlan =
            doubleArrayOf(
                5.0,
                11.0,
                7.0,
                3.0,
                26.0,
                2048.0,
                512.0,
                9.0,
            ).toEncodeOperationWorkPlan()

        assertEquals(5L, workPlan.preparingInputWorkUnits)
        assertEquals(11L, workPlan.renderingPcmWorkUnits)
        assertEquals(7L, workPlan.postprocessingWorkUnits)
        assertEquals(3L, workPlan.finalizingWorkUnits)
        assertEquals(26L, workPlan.totalWorkUnits)
        assertEquals(2048L, workPlan.estimatedPcmSampleCount)
        assertEquals(512L, workPlan.payloadByteCount)
        assertEquals(9L, workPlan.segmentCount)
    }

    @Test
    fun `zero native handle maps to internal failure`() {
        val result = EncodeAudioResult.Failed(BagApiCodes.ERROR_INTERNAL)

        assertEquals(BagApiCodes.ERROR_INTERNAL, result.errorCode)
    }

    @Test
    fun `empty successful pcm result maps to internal failure`() {
        val result = EncodedAudioPayloadResult(pcm = shortArrayOf()).toEncodeSuccessOrFailureResult()

        assertTrue(result is EncodeAudioResult.Failed)
        assertEquals(BagApiCodes.ERROR_INTERNAL, (result as EncodeAudioResult.Failed).errorCode)
    }

    @Test
    fun `non-empty successful pcm result maps to success`() {
        val pcm = shortArrayOf(1, 2, 3)
        val result = EncodedAudioPayloadResult(pcm = pcm).toEncodeSuccessOrFailureResult()

        assertTrue(result is EncodeAudioResult.Success)
        assertEquals(pcm.toList(), (result as EncodeAudioResult.Success).pcm.toList())
    }
}
