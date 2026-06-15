package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTabTest {
    @Test
    fun fromAutomationId_mapsDataTabId() {
        assertEquals(AppTab.Data, AppTab.fromAutomationId("data"))
        assertEquals(AppTab.Voice, AppTab.fromAutomationId("voice"))
        assertEquals(AppTab.Library, AppTab.fromAutomationId("saved"))
        assertEquals(AppTab.Config, AppTab.fromAutomationId("settings"))
    }
}
