package com.bag.audioandroid.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSpeedPolicyTest {
    @Test
    fun `high playback speeds use rendered pcm path`() {
        assertFalse(shouldRenderSpeedAdjustedPcm(PlaybackSpeedNormal))
        assertTrue(shouldRenderSpeedAdjustedPcm(1.5f))
        assertTrue(shouldRenderSpeedAdjustedPcm(2.0f))
        assertTrue(shouldRenderSpeedAdjustedPcm(4.0f))
    }
}
