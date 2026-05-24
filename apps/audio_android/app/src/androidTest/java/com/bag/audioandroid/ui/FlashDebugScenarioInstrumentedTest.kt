package com.bag.audioandroid.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import com.bag.audioandroid.MainActivity
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.screen.FlashSignalVisualizationMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FlashDebugScenarioInstrumentedTest(
    private val style: FlashVoicingStyleOption,
) {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun debugScenarioCreatesPlayableFlashVisualFromFixedText() {
        val scenario =
            ActivityScenario.launch<MainActivity>(
                Intent(Intent.ACTION_MAIN)
                    .setClassName("com.bag.audioandroid", MainActivity::class.java.name)
                    .setAction(FlashDebugScenario.Action)
                    .putExtra(FlashDebugScenario.ExtraText, FlashDebugScenario.DefaultText)
                    .putExtra(FlashDebugScenario.ExtraStyle, style.id)
                    .putExtra(FlashDebugScenario.ExtraVisual, FlashSignalVisualizationMode.Lanes.name)
                    .putExtra(FlashDebugScenario.ExtraEncode, true)
                    .putExtra(FlashDebugScenario.ExtraPlay, true)
                    .putExtra(FlashDebugScenario.ExtraPlayDurationMs, 6_000L),
            )
        try {
            composeRule.waitUntil(timeoutMillis = 90_000) {
                composeRule
                    .onAllNodesWithTag("player-detail-sheet-content")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("player-detail-sheet-content").assertIsDisplayed()
            composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
            composeRule.onNodeWithTag("flash-visualization-mode-switcher").assertIsDisplayed()
            composeRule.onNodeWithTag("flash-visualization-mode-lanes").assertIsDisplayed()
        } finally {
            scenario.close()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun styles(): List<Array<Any>> = FlashVoicingStyleOption.entries.map { style -> arrayOf(style) }
    }
}
