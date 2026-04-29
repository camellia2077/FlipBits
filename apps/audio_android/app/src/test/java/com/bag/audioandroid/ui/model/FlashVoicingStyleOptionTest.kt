package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashVoicingStyleOptionTest {
    @Test
    fun `fromId maps current emotion ids`() {
        assertEquals(FlashVoicingStyleOption.Steady, FlashVoicingStyleOption.fromId("steady"))
        assertEquals(FlashVoicingStyleOption.Hostile, FlashVoicingStyleOption.fromId("hostile"))
        assertEquals(FlashVoicingStyleOption.Litany, FlashVoicingStyleOption.fromId("litany"))
        assertEquals(FlashVoicingStyleOption.Collapse, FlashVoicingStyleOption.fromId("collapse"))
    }

    @Test
    fun `fromId falls back for unknown ids`() {
        assertEquals(FlashVoicingStyleOption.Steady, FlashVoicingStyleOption.fromId(null))
        assertEquals(FlashVoicingStyleOption.Steady, FlashVoicingStyleOption.fromId("unknown"))
    }

    @Test
    fun `emotion presets carry separate signal and voicing axes`() {
        assertEquals(FlashSignalProfileWire.STEADY, FlashVoicingStyleOption.Steady.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.STEADY, FlashVoicingStyleOption.Steady.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.HOSTILE, FlashVoicingStyleOption.Hostile.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.HOSTILE, FlashVoicingStyleOption.Hostile.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.LITANY, FlashVoicingStyleOption.Litany.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.LITANY, FlashVoicingStyleOption.Litany.voicingFlavorValue)
        assertEquals(FlashSignalProfileWire.COLLAPSE, FlashVoicingStyleOption.Collapse.signalProfileValue)
        assertEquals(FlashVoicingFlavorWire.COLLAPSE, FlashVoicingStyleOption.Collapse.voicingFlavorValue)
    }

    @Test
    fun `helpers expose behavior instead of wire-value branching`() {
        assertEquals(3, FlashVoicingStyleOption.Steady.flashVisualActiveWindowBucketCount)
        assertEquals(8, FlashVoicingStyleOption.Litany.flashVisualActiveWindowBucketCount)
        assertEquals(false, FlashVoicingStyleOption.Steady.usesLongCadencePayload)
        assertEquals(true, FlashVoicingStyleOption.Litany.usesLongCadencePayload)
    }
}
