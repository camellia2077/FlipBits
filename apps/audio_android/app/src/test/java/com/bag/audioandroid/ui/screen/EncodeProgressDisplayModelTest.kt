package com.bag.audioandroid.ui.screen

import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.EncodeOperationSnapshot
import com.bag.audioandroid.domain.EncodeOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncodeProgressDisplayModelTest {
    @Test
    fun `display model is derived from operation snapshot`() {
        val model =
            EncodeOperationSnapshot.Initial
                .copy(
                    state = EncodeOperationState.Running,
                    phase = AudioEncodePhase.RenderingPcm,
                    overallProgress0To1 = 0.625f,
                ).toEncodeProgressDisplayModel()

        requireNotNull(model)
        assertEquals(AudioEncodePhase.RenderingPcm, model.phase)
        assertEquals(0.625f, model.progress0To1)
        assertEquals(63, model.percent)
    }

    @Test
    fun `null snapshot has no display model`() {
        assertNull(null.toEncodeProgressDisplayModel())
    }
}
