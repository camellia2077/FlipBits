package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.state.VoiceSessionState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceTabScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `clip mode shows merged workflow card and result card`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        inputPcm = shortArrayOf(1, 2, 3, 4),
                        processedPcm = shortArrayOf(5, 6, 7, 8),
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-workflow-clip").assertExists()
        composeRule.onNodeWithTag("voice-clip-workflow-card").assertExists()
        composeRule.onNodeWithTag("voice-result-card").assertExists()
        composeRule.onNodeWithTag("voice-result-input-track").assertExists()
        composeRule.onNodeWithTag("voice-result-output-track").assertExists()
        composeRule.onNodeWithTag("voice-result-output-track-play").assertExists()
        composeRule.onNodeWithTag("voice-result-share-button").assertExists()
        composeRule.onNodeWithTag("voice-result-download-button").assertExists()
        composeRule.onNodeWithTag("voice-preset-selector").assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.voice_preset_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(VoiceFxPresetOption.Binharic.labelResId)).assertIsDisplayed()
        composeRule.onNodeWithTag("voice-process-button").assertExists()
        composeRule.onNodeWithTag("voice-process-hint").assertDoesNotExist()
        composeRule.onNodeWithTag("voice-record-processing-mode-selector").assertExists()
        composeRule.onNodeWithTag("voice-record-processing-mode-after_recording").assertExists()
        composeRule.onNodeWithTag("voice-record-processing-mode-while_recording").assertExists()
        composeRule.onNodeWithTag("voice-start-recording-button").assertExists()
        composeRule.onNodeWithTag("voice-live-workflow-card").assertDoesNotExist()
    }

    @Test
    fun `upload input hides record processing mode selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedInputSource = VoiceInputSourceOption.Upload,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-record-processing-mode-selector").assertDoesNotExist()
        composeRule.onNodeWithTag("voice-upload-button").assertExists()
    }

    @Test
    fun `record processing mode selector emits while recording option`() {
        var selectedMode: VoiceRecordProcessingModeOption? = null
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Single,
                        selectedPreset = VoiceFxPresetOption.MachineVoice,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = { selectedMode = it },
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-record-processing-mode-while_recording").performClick()

        assertEquals(VoiceRecordProcessingModeOption.WhileRecording, selectedMode)
    }

    @Test
    fun `live mode hides clip-only controls`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-live-workflow-card").assertIsDisplayed()
        composeRule.onNodeWithTag("voice-workflow-live").assertExists()
        composeRule.onNodeWithTag("voice-clip-workflow-card").assertDoesNotExist()
        composeRule.onNodeWithTag("voice-result-card").assertDoesNotExist()
        composeRule.onNodeWithTag("voice-process-button").assertDoesNotExist()
        composeRule
            .onNodeWithText(
                composeRule.activity.getString(VoiceInputSourceOption.Record.labelResId),
            ).assertDoesNotExist()
    }

    @Test
    fun `first live selection shows preview dialog before switching`() {
        var selectedMode: VoiceWorkflowModeOption? = null
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = { selectedMode = it },
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-workflow-live").performClick()

        composeRule.onNodeWithTag("voice-live-preview-dialog").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.voice_live_preview_dialog_title)).assertIsDisplayed()
        assertEquals(null, selectedMode)

        composeRule.onNodeWithTag("voice-live-preview-try-live").performClick()

        assertEquals(VoiceWorkflowModeOption.Live, selectedMode)
    }

    @Test
    fun `live preview dialog use clip keeps clip mode selected`() {
        var selectedMode: VoiceWorkflowModeOption? = null
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = { selectedMode = it },
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-workflow-live").performClick()
        composeRule.onNodeWithTag("voice-live-preview-use-clip").performClick()

        assertEquals(VoiceWorkflowModeOption.Clip, selectedMode)
    }

    @Test
    fun `binharic clip mode shows voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Dual,
                        selectedPreset = VoiceFxPresetOption.Binharic,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertExists()
        composeRule.onNodeWithTag("binharic-voicing-style-description").assertExists()
    }

    @Test
    fun `binharic live mode shows voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                        selectedTrackMode = VoiceTrackModeOption.Dual,
                        selectedPreset = VoiceFxPresetOption.Binharic,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertExists()
        composeRule.onNodeWithTag("binharic-voicing-style-description").assertExists()
    }

    @Test
    fun `raw constant dual track preset shows voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Dual,
                        selectedPreset = VoiceFxPresetOption.RawConstant,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertExists()
        composeRule.onNodeWithTag("binharic-voicing-style-description").assertExists()
    }

    @Test
    fun `voice trigger dual track preset shows voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Dual,
                        selectedPreset = VoiceFxPresetOption.VoiceTrigger,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertExists()
        composeRule.onNodeWithTag("binharic-voicing-style-description").assertExists()
    }

    @Test
    fun `non binharic clip mode hides voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Single,
                        selectedPreset = VoiceFxPresetOption.MachineVoice,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertDoesNotExist()
    }

    @Test
    fun `non binharic live mode hides voicing selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                        selectedTrackMode = VoiceTrackModeOption.Single,
                        selectedPreset = VoiceFxPresetOption.MachineVoice,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertDoesNotExist()
    }

    @Test
    fun `single track mode hides binharic controls and shows selector`() {
        composeRule.setContent {
            VoiceTabScreen(
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                session =
                    VoiceSessionState(
                        hasRecordPermission = true,
                        selectedTrackMode = VoiceTrackModeOption.Single,
                        selectedPreset = VoiceFxPresetOption.MachineVoice,
                    ),
                onRequestRecordPermission = {},
                onWorkflowModeSelected = {},
                onTrackModeSelected = {},
                onInputSourceSelected = {},
                onRecordProcessingModeSelected = {},
                onPresetSelected = {},
                onSubvoiceStyleSelected = {},
                onImportAudio = {},
                onStartRecording = {},
                onStopRecording = {},
                onStartLive = {},
                onStopLive = {},
                onProcess = {},
                onTogglePreview = {},
                onExportResult = {},
                onClear = {},
            )
        }

        composeRule.onNodeWithTag("voice-track-mode-selector").assertExists()
        composeRule.onNodeWithTag("voice-track-mode-single").assertExists()
        composeRule.onNodeWithTag("voice-track-mode-dual").assertExists()
        composeRule.onNodeWithTag("binharic-voicing-style-selector").assertDoesNotExist()
    }
}
