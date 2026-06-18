package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceSubvoiceStyleSelectorSectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `selector row shows current style and description`() {
        composeRule.setContent {
            VoiceSubvoiceStyleSelectorSection(
                enabled = true,
                selectedStyle = VoiceFxSubvoiceStyleOption.Standard,
                onStyleSelected = {},
            )
        }

        composeRule.onNodeWithTag("binaric-cant-voicing-style-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("binaric-cant-voicing-style-description").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.voice_voicing_style_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(VoiceFxSubvoiceStyleOption.Standard.labelResId)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(VoiceFxSubvoiceStyleOption.Standard.descriptionResId)).assertIsDisplayed()
    }

    @Test
    fun `picker sheet shows all voicing styles and selection callback fires`() {
        var selected = VoiceFxSubvoiceStyleOption.Standard
        composeRule.setContent {
            VoiceSubvoiceStylePickerSheet(
                selectedStyle = selected,
                onStyleSelected = { selected = it },
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.voice_voicing_style_sheet_title)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-standard", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-litany", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-hostility", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-collapse", useUnmergedTree = true).assertCountEquals(1)
        composeRule
            .onNodeWithTag("binaric-cant-voicing-style-list")
            .performScrollToNode(hasTestTag("binaric-cant-voicing-style-zeal"))
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-zeal", useUnmergedTree = true).assertCountEquals(1)
        composeRule
            .onNodeWithTag("binaric-cant-voicing-style-list")
            .performScrollToNode(hasTestTag("binaric-cant-voicing-style-void"))
        composeRule.onAllNodesWithTag("binaric-cant-voicing-style-void", useUnmergedTree = true).assertCountEquals(1)

        composeRule.onNodeWithTag("binaric-cant-voicing-style-hostility", useUnmergedTree = true).performClick()

        assertEquals(VoiceFxSubvoiceStyleOption.Hostility, selected)
    }
}
