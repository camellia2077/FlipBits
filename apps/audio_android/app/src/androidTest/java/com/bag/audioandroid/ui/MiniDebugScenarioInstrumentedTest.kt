package com.bag.audioandroid.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import com.bag.audioandroid.MainActivity
import com.bag.audioandroid.ui.model.MorseSpeedOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MiniDebugScenarioInstrumentedTest(
    private val speed: MorseSpeedOption,
) {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun debugScenarioCreatesPlayableMiniVisualFromFixedText() {
        ActivityScenario
            .launch<MainActivity>(
                Intent(Intent.ACTION_MAIN)
                    .setClassName("com.bag.audioandroid", MainActivity::class.java.name)
                    .setAction(MiniDebugScenario.Action)
                    .putExtra(MiniDebugScenario.ExtraText, MiniDebugScenario.DefaultText)
                    .putExtra(MiniDebugScenario.ExtraSpeed, speed.id)
                    .putExtra(MiniDebugScenario.ExtraEncode, true)
                    .putExtra(MiniDebugScenario.ExtraPlay, true)
                    .putExtra(MiniDebugScenario.ExtraPlayDurationMs, 6_000L),
            ).use {
                composeRule.waitUntil(timeoutMillis = 90_000) {
                    composeRule
                        .onAllNodesWithTag("player-detail-sheet-content")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithTag("player-detail-sheet-content").assertIsDisplayed()
                composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
                composeRule.onNodeWithTag("morse-timeline-visualizer").assertIsDisplayed()
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun speeds(): List<Array<Any>> = MorseSpeedOption.entries.map { speed -> arrayOf(speed) }
    }
}
