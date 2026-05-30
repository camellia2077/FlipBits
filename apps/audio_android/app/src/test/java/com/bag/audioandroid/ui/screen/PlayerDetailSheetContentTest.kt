package com.bag.audioandroid.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import com.bag.audioandroid.ui.LyricsNavigatorReadingModel
import com.bag.audioandroid.ui.buildLyricsNavigatorReadingModel
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.MiniPlayerLeadingIcon
import com.bag.audioandroid.ui.model.MiniPlayerSource
import com.bag.audioandroid.ui.model.MiniPlayerUiModel
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.resolveSeekSampleForReadingLine
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PlayerDetailSheetContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `player detail sheet renders playback section inside real scroll structure`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Flash"),
                        subtitle =
                            UiText.Resource(
                                R.string.audio_mini_player_generated_flash_subtitle,
                                listOf(UiText.Resource(FlashVoicingStyleOption.Litany.labelResId), "0:44"),
                            ),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.Litany,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = false,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("player-detail-sheet-content").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-section").assertIsDisplayed()
        composeRule.onNodeWithTag("playback-display-switcher").assertIsDisplayed()
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.audio_player_detail_now_playing)).assertCountEquals(0)
    }

    @Test
    fun `audio info button opens dialog with transport mode and duration`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Pro"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Pro,
                        isFlashMode = false,
                        flashVoicingStyle = null,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_info_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.transport_mode_pro_label)).assertIsDisplayed()
        composeRule.onNodeWithText("0:44").assertIsDisplayed()
        composeRule.onAllNodesWithTag("audio-info-user-section", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-technical-section", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-row-sample-rate", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("audio-info-row-frame-samples", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `saved audio info dialog shows file size`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Saved"),
                        subtitle = UiText.Plain("saved"),
                        leadingIcon = MiniPlayerLeadingIcon.Saved,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.Litany,
                        source = MiniPlayerSource.Saved,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = false,
                followData = sampleFollowData(),
                savedAudioItem =
                    SavedAudioItem(
                        itemId = "1",
                        displayName = "Saved.wav",
                        uriString = "content://saved/1",
                        modeWireName = TransportModeOption.Flash.wireName,
                        durationMs = 44_000L,
                        savedAtEpochSeconds = 1_700_000_000L,
                        flashVoicingStyle = FlashVoicingStyleOption.Litany,
                        fileSizeBytes = 12_345L,
                    ),
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onAllNodesWithTag("audio-info-row-file-size", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithText("0.01 MB").assertIsDisplayed()
    }

    @Test
    fun `generated audio info dialog shows estimated wav file size`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Pro"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Pro,
                        isFlashMode = false,
                        flashVoicingStyle = null,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onAllNodesWithTag("audio-info-row-file-size", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithText("0.01 MB").assertIsDisplayed()
    }

    @Test
    fun `flash audio info dialog shows voicing style`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Flash"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.Litany,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = sampleFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.audio_action_open_audio_info)).performClick()
        composeRule.onAllNodesWithTag("audio-info-row-flash-voicing-style", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `preview waveform keeps flash visualizer and real follow timeline`() {
        setPreviewPlayerDetailContent(initialDisplayMode = PlaybackDisplayMode.Visual)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_flash_visualizer_mode_lanes)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_flash_visualizer_mode_pulse)).assertIsDisplayed()
    }

    @Test
    fun `preview waveform keeps lyrics on real follow timeline`() {
        setPreviewPlayerDetailContent(initialDisplayMode = PlaybackDisplayMode.Lyrics)
        composeRule.onAllNodesWithTag("follow-token-active", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `preview waveform mix mode shows visualizer and token strip without lyrics preview`() {
        setPreviewPlayerDetailContent(initialDisplayMode = PlaybackDisplayMode.Mix)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_flash_visualizer_mode_lanes)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_flash_visualizer_mode_pulse)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("playback-follow-section").assertCountEquals(1)
        composeRule.onAllNodesWithTag("playback-lyrics-section").assertCountEquals(0)
    }

    @Test
    fun `mini visual shows morse mode switcher and can switch between horizontal and vertical playback`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Mini"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Mini,
                        isFlashMode = false,
                        flashVoicingStyle = null,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 900,
                totalSamples = 4_800,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                frameSamples = 100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = false,
                followData = sampleMiniFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                initialDisplayMode = PlaybackDisplayMode.Visual,
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_morse_visualizer_mode_vertical)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_morse_visualizer_mode_horizontal)).assertIsDisplayed()
        composeRule.onNodeWithTag("morse-horizontal-visualizer").assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_morse_visualizer_mode_vertical)).performClick()

        composeRule.onNodeWithTag("morse-timeline-visualizer").assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.audio_morse_visualizer_mode_horizontal)).performClick()

        composeRule.onNodeWithTag("morse-horizontal-visualizer").assertIsDisplayed()
    }

    @Test
    fun `mix mode token follow uses visual displayed samples`() {
        assertEquals(
            409,
            playbackFollowSectionDisplayedSamples(
                playbackDisplayMode = PlaybackDisplayMode.Mix,
                displayedSamples = 10_000,
                visualDisplayedSamples = 409,
            ),
        )
    }

    @Test
    fun `mix mode token follow prefers shared flash playback sample when available`() {
        assertEquals(
            26390,
            playbackFollowSectionDisplayedSamples(
                playbackDisplayMode = PlaybackDisplayMode.Mix,
                displayedSamples = 10_000,
                visualDisplayedSamples = 409,
                sharedFlashPlaybackSampleState =
                    FlashVisualPlaybackSampleState(
                        rawSample = 35_197f,
                        displayedSample = 26_390f,
                    ),
            ),
        )
    }

    @Test
    fun `lyrics mode token follow keeps raw displayed samples`() {
        assertEquals(
            10_000,
            playbackFollowSectionDisplayedSamples(
                playbackDisplayMode = PlaybackDisplayMode.Lyrics,
                displayedSamples = 10_000,
                visualDisplayedSamples = 409,
            ),
        )
    }

    @Test
    fun `ultra visual recovers lyrics preview gap like signal visuals`() {
        assertEquals(true, TransportModeOption.Ultra.shouldRecoverVisualLyrics(PlaybackDisplayMode.Visual))
        assertEquals(false, TransportModeOption.Ultra.shouldRecoverVisualLyrics(PlaybackDisplayMode.Lyrics))
        assertEquals(false, TransportModeOption.Pro.shouldRecoverVisualLyrics(PlaybackDisplayMode.Visual))
    }

    @Test
    fun `ultra visual can use recovered gap for more lyrics preview lines`() {
        assertEquals(
            7,
            computeCompactLyricsVisibleLineCount(
                transportMode = TransportModeOption.Ultra,
                playbackDisplayMode = PlaybackDisplayMode.Visual,
                prefersWrappedLines = false,
                effectiveExtraLyricsRecoveryHeight = 144.dp,
                tokenStripHeightDp = null,
                applyLyricsPreviewBonusLine = false,
            ),
        )
    }

    @Test
    fun `player detail exposes lyrics expand toggle`() {
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Mini"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 44_000L,
                        transportMode = TransportModeOption.Mini,
                        isFlashMode = false,
                        flashVoicingStyle = null,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 7,
                totalSamples = 12,
                isScrubbing = false,
                waveformPcm = shortArrayOf(1, 2, 3, 4, 5, 6),
                sampleRateHz = 44_100,
                displayedTime = "0:07",
                totalTime = "0:12",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = false,
                followData =
                    sampleFollowData().copy(
                        lyricLines = listOf("ASH BELL RITE"),
                        lineTokenRanges =
                            listOf(
                                com.bag.audioandroid.domain
                                    .TextFollowLineTokenRangeViewData(0, 0, 3),
                            ),
                        lyricLineFollowAvailable = true,
                    ),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
            )
        }

        composeRule.onNodeWithTag("playback-token-context-tape-list").assertExists()
        composeRule.onNodeWithTag("playback-lyrics-open-navigator").assertExists()
        composeRule.onAllNodesWithTag("playback-lyrics-expand-toggle").assertCountEquals(0)
    }

    @Test
    fun `lyrics navigator back closes full screen page`() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) {
                LyricsNavigatorScaffold(
                    followData =
                        sampleFollowData().copy(
                            lyricLines = listOf("ASH BELL RITE", "LAMP TIDE HUSH"),
                            lineTokenRanges =
                                listOf(
                                    com.bag.audioandroid.domain
                                        .TextFollowLineTokenRangeViewData(0, 0, 3),
                                    com.bag.audioandroid.domain
                                        .TextFollowLineTokenRangeViewData(1, 3, 3),
                                ),
                            lyricLineFollowAvailable = true,
                        ),
                    displayedSamples = 7,
                    totalSamples = 12,
                    displayedTime = "0:07",
                    totalTime = "0:12",
                    isPlaying = false,
                    isScrubbing = false,
                    playbackSequenceMode = PlaybackSequenceMode.Normal,
                    playbackSpeed = 1.0f,
                    canSkipPrevious = false,
                    canSkipNext = false,
                    transportMode = TransportModeOption.Mini,
                    durationMs = 44_000L,
                    sampleRateHz = 44_100,
                    frameSamples = 2205,
                    wavAudioInfo = com.bag.audioandroid.domain.WavAudioInfo.Empty,
                    onBack = { visible = false },
                    onTogglePlayback = {},
                    onSkipToPreviousTrack = {},
                    onSkipToNextTrack = {},
                    onPlaybackSequenceModeSelected = {},
                    onPlaybackSpeedSelected = {},
                    onScrubStarted = {},
                    onScrubChanged = {},
                    onScrubFinished = {},
                    onSeekToSample = {},
                )
            }
        }

        composeRule.onNodeWithTag("lyrics-navigator-back").performClick()
        composeRule.runOnIdle {
            assertEquals(false, visible)
        }
    }

    @Test
    fun `lyrics navigator uses plain full text reading view`() {
        composeRule.setContent {
            LyricsNavigatorScaffold(
                followData =
                    PayloadFollowViewData(
                        textTokens = listOf("ASH", "BELL", "RITE", "LAMP", "TIDE", "HUSH"),
                        textTokenTimeline =
                            listOf(
                                TextFollowTimelineEntry(0, 4, 0),
                                TextFollowTimelineEntry(4, 4, 1),
                                TextFollowTimelineEntry(8, 4, 2),
                                TextFollowTimelineEntry(12, 4, 3),
                                TextFollowTimelineEntry(16, 4, 4),
                                TextFollowTimelineEntry(20, 4, 5),
                            ),
                        lyricLines = listOf("ASH BELL RITE", "LAMP TIDE HUSH"),
                        lineTokenRanges =
                            listOf(
                                com.bag.audioandroid.domain
                                    .TextFollowLineTokenRangeViewData(0, 0, 3),
                                com.bag.audioandroid.domain
                                    .TextFollowLineTokenRangeViewData(1, 3, 3),
                            ),
                        textFollowAvailable = true,
                        lyricLineFollowAvailable = true,
                        followAvailable = true,
                    ),
                displayedSamples = 7,
                totalSamples = 24,
                displayedTime = "0:07",
                totalTime = "0:24",
                isPlaying = false,
                isScrubbing = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                transportMode = TransportModeOption.Mini,
                durationMs = 44_000L,
                sampleRateHz = 44_100,
                frameSamples = 2205,
                wavAudioInfo = com.bag.audioandroid.domain.WavAudioInfo.Empty,
                onBack = {},
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                onSeekToSample = {},
            )
        }

        composeRule.onNodeWithTag("lyrics-navigator-reading-text").assertExists()
        composeRule.onAllNodesWithTag("playback-lyrics-full-list", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `lyrics navigator prefers full reading model over windowed follow data`() {
        composeRule.setContent {
            LyricsNavigatorScaffold(
                followData =
                    PayloadFollowViewData(
                        lyricLines = listOf("WINDOW ONLY"),
                        textFollowAvailable = true,
                        followAvailable = true,
                    ),
                navigatorReadingModel =
                    LyricsNavigatorReadingModel(
                        text = "LINE ONE\nLINE TWO\nLINE THREE",
                        sampleAtOffset = IntArray("LINE ONE\nLINE TWO\nLINE THREE".length) { 12 },
                    ),
                displayedSamples = 7,
                totalSamples = 24,
                displayedTime = "0:07",
                totalTime = "0:24",
                isPlaying = false,
                isScrubbing = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                transportMode = TransportModeOption.Mini,
                durationMs = 44_000L,
                sampleRateHz = 44_100,
                frameSamples = 2205,
                wavAudioInfo = com.bag.audioandroid.domain.WavAudioInfo.Empty,
                onBack = {},
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                onSeekToSample = {},
            )
        }

        composeRule.onNodeWithText("LINE ONE").assertExists()
        composeRule.onNodeWithText("LINE TWO").assertExists()
        composeRule.onNodeWithText("LINE THREE").assertExists()
        composeRule.onAllNodesWithText("WINDOW ONLY", substring = true).assertCountEquals(0)
    }

    @Test
    fun `lyrics navigator reading model preserves full parsed text`() {
        val model =
            buildLyricsNavigatorReadingModel(
                PayloadFollowViewData(
                    textCharacters =
                        listOf(
                            com.bag.audioandroid.domain
                                .TextFollowCharacterViewData(0, 0, 0, 1, 0, 2, text = "A"),
                            com.bag.audioandroid.domain.TextFollowCharacterViewData(
                                0,
                                1,
                                1,
                                1,
                                2,
                                2,
                                kindCode = com.bag.audioandroid.domain.TextFollowCharacterKind.Space.wireValue,
                                text = " ",
                            ),
                            com.bag.audioandroid.domain
                                .TextFollowCharacterViewData(1, 0, 0, 1, 4, 2, text = "B"),
                            com.bag.audioandroid.domain.TextFollowCharacterViewData(
                                1,
                                1,
                                1,
                                1,
                                6,
                                2,
                                kindCode = com.bag.audioandroid.domain.TextFollowCharacterKind.Newline.wireValue,
                                text = "\n",
                            ),
                            com.bag.audioandroid.domain
                                .TextFollowCharacterViewData(2, 0, 0, 1, 8, 2, text = "C"),
                        ),
                    textFollowAvailable = true,
                    followAvailable = true,
                ),
            )

        assertEquals("A B\nC", model.text)
        assertEquals(5, model.sampleAtOffset.size)
        assertEquals(0, model.sampleAtOffset[0])
        assertEquals(2, model.sampleAtOffset[1])
        assertEquals(4, model.sampleAtOffset[2])
        assertEquals(6, model.sampleAtOffset[3])
        assertEquals(8, model.sampleAtOffset[4])
    }

    @Test
    fun `lyrics navigator reading line seeks to first visible character sample`() {
        val sample =
            resolveSeekSampleForReadingLine(
                text = "AB\nCD",
                sampleAtOffset = intArrayOf(10, 12, -1, 20, 22),
                lineStart = 3,
                lineEnd = 5,
            )

        assertEquals(20, sample)
    }

    @Test
    fun `lyrics navigator reading model inserts conservative punctuation breaks`() {
        val model =
            buildLyricsNavigatorReadingModel(
                PayloadFollowViewData(
                    lyricLines = listOf("第一句。第二句，第三句需要更长一点，第四句。"),
                    lineTokenRanges =
                        listOf(
                            com.bag.audioandroid.domain
                                .TextFollowLineTokenRangeViewData(0, 0, 1),
                        ),
                    textTokenTimeline = listOf(TextFollowTimelineEntry(12, 8, 0)),
                    lyricLineFollowAvailable = true,
                    followAvailable = true,
                ),
            )
        val formatted = formatLyricsNavigatorReadingModelForDisplay(model)

        assertEquals(
            "第一句。\n第二句，第三句需要更长一点，\n第四句。",
            formatted.text,
        )
    }

    private fun setPreviewPlayerDetailContent(initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics) {
        val flashVisualSegments =
            listOf(
                FlashSignalToneSegment(0, 5_000, FskDominantTone.Low),
                FlashSignalToneSegment(5_000, 10_000, FskDominantTone.High),
                FlashSignalToneSegment(15_000, 20_000, FskDominantTone.Low),
            )
        composeRule.setContent {
            PlayerDetailSheetContent(
                miniPlayerModel =
                    MiniPlayerUiModel(
                        title = UiText.Plain("Flash"),
                        subtitle = UiText.Plain("generated"),
                        leadingIcon = MiniPlayerLeadingIcon.Generated,
                        durationMs = 100_000L,
                        transportMode = TransportModeOption.Flash,
                        isFlashMode = true,
                        flashVoicingStyle = FlashVoicingStyleOption.Litany,
                        source = MiniPlayerSource.Generated,
                    ),
                displayedSamples = 10_000,
                waveformDisplayedSamples = 409,
                totalSamples = 100_000,
                isScrubbing = false,
                waveformPcm = ShortArray(4096) { index -> (index % 64).toShort() },
                isWaveformPreview = true,
                sampleRateHz = 44_100,
                displayedTime = "0:10",
                totalTime = "1:40",
                isPlaying = false,
                playbackSequenceMode = PlaybackSequenceMode.Normal,
                playbackSpeed = 1.0f,
                canSkipPrevious = false,
                canSkipNext = false,
                canExportGeneratedAudio = true,
                followData = longTimelineFollowData(),
                savedAudioItem = null,
                onTogglePlayback = {},
                onSkipToPreviousTrack = {},
                onSkipToNextTrack = {},
                onPlaybackSequenceModeSelected = {},
                onPlaybackSpeedSelected = {},
                onExportGeneratedAudio = {},
                onExportGeneratedAudioToDocument = {},
                onShareSavedAudio = null,
                onOpenSavedAudioSheet = {},
                onScrubStarted = {},
                onScrubChanged = {},
                onScrubFinished = {},
                initialDisplayMode = initialDisplayMode,
            )
        }
    }

    private fun sampleFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("ASH", "BELL", "RITE"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 4, 0),
                    TextFollowTimelineEntry(4, 4, 1),
                    TextFollowTimelineEntry(8, 4, 2),
                ),
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 0,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 0,
                        byteCount = 1,
                        hexText = "41",
                        binaryText = "01000001",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 1,
                        sampleCount = 1,
                        byteIndexWithinToken = 1,
                        byteOffset = 1,
                        byteCount = 1,
                        hexText = "53",
                        binaryText = "01010011",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 2,
                        sampleCount = 2,
                        byteIndexWithinToken = 2,
                        byteOffset = 2,
                        byteCount = 1,
                        hexText = "48",
                        binaryText = "01001000",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 4,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 3,
                        byteCount = 1,
                        hexText = "42",
                        binaryText = "01000010",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 5,
                        sampleCount = 1,
                        byteIndexWithinToken = 1,
                        byteOffset = 4,
                        byteCount = 1,
                        hexText = "45",
                        binaryText = "01000101",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 6,
                        sampleCount = 1,
                        byteIndexWithinToken = 2,
                        byteOffset = 5,
                        byteCount = 1,
                        hexText = "4C",
                        binaryText = "01001100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 7,
                        sampleCount = 1,
                        byteIndexWithinToken = 3,
                        byteOffset = 6,
                        byteCount = 1,
                        hexText = "4C",
                        binaryText = "01001100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 2,
                        startSample = 8,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 7,
                        byteCount = 1,
                        hexText = "52",
                        binaryText = "01010010",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 2,
                        startSample = 9,
                        sampleCount = 1,
                        byteIndexWithinToken = 1,
                        byteOffset = 8,
                        byteCount = 1,
                        hexText = "49",
                        binaryText = "01001001",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 2,
                        startSample = 10,
                        sampleCount = 1,
                        byteIndexWithinToken = 2,
                        byteOffset = 9,
                        byteCount = 1,
                        hexText = "54",
                        binaryText = "01010100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 2,
                        startSample = 11,
                        sampleCount = 1,
                        byteIndexWithinToken = 3,
                        byteOffset = 10,
                        byteCount = 1,
                        hexText = "45",
                        binaryText = "01000101",
                    ),
                ),
            textFollowAvailable = true,
            followAvailable = true,
        )

    private fun sampleMiniFollowData(): PayloadFollowViewData =
        sampleFollowData().copy(
            binaryGroupTimeline =
                listOf(
                    PayloadFollowBinaryGroupTimelineEntry(
                        startSample = 0,
                        sampleCount = 100,
                        groupIndex = 0,
                        bitOffset = 0,
                        bitCount = 1,
                    ),
                    PayloadFollowBinaryGroupTimelineEntry(
                        startSample = 200,
                        sampleCount = 300,
                        groupIndex = 1,
                        bitOffset = 1,
                        bitCount = 1,
                    ),
                    PayloadFollowBinaryGroupTimelineEntry(
                        startSample = 700,
                        sampleCount = 100,
                        groupIndex = 2,
                        bitOffset = 2,
                        bitCount = 1,
                    ),
                    PayloadFollowBinaryGroupTimelineEntry(
                        startSample = 1_000,
                        sampleCount = 300,
                        groupIndex = 3,
                        bitOffset = 3,
                        bitCount = 1,
                    ),
                ),
            totalPcmSampleCount = 4_800,
        )

    private fun longTimelineFollowData(): PayloadFollowViewData =
        PayloadFollowViewData(
            textTokens = listOf("ASH", "BELL", "RITE"),
            textTokenTimeline =
                listOf(
                    TextFollowTimelineEntry(0, 5_000, 0),
                    TextFollowTimelineEntry(9_000, 5_000, 1),
                    TextFollowTimelineEntry(20_000, 5_000, 2),
                ),
            textRawDisplayUnits =
                listOf(
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 0,
                        startSample = 0,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 0,
                        byteCount = 1,
                        hexText = "41",
                        binaryText = "01000001",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 9_000,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 1,
                        byteCount = 1,
                        hexText = "42",
                        binaryText = "01000010",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 9_001,
                        sampleCount = 1,
                        byteIndexWithinToken = 1,
                        byteOffset = 2,
                        byteCount = 1,
                        hexText = "45",
                        binaryText = "01000101",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 9_002,
                        sampleCount = 1,
                        byteIndexWithinToken = 2,
                        byteOffset = 3,
                        byteCount = 1,
                        hexText = "4C",
                        binaryText = "01001100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 1,
                        startSample = 9_003,
                        sampleCount = 1,
                        byteIndexWithinToken = 3,
                        byteOffset = 4,
                        byteCount = 1,
                        hexText = "4C",
                        binaryText = "01001100",
                    ),
                    TextFollowRawDisplayUnitViewData(
                        tokenIndex = 2,
                        startSample = 20_000,
                        sampleCount = 1,
                        byteIndexWithinToken = 0,
                        byteOffset = 5,
                        byteCount = 1,
                        hexText = "52",
                        binaryText = "01010010",
                    ),
                ),
            binaryTokens = listOf("0", "1", "0", "1"),
            binaryGroupTimeline =
                listOf(
                    PayloadFollowBinaryGroupTimelineEntry(0, 5_000, 0, 0, 1),
                    PayloadFollowBinaryGroupTimelineEntry(5_000, 5_000, 1, 1, 1),
                    PayloadFollowBinaryGroupTimelineEntry(10_000, 5_000, 2, 2, 1),
                    PayloadFollowBinaryGroupTimelineEntry(15_000, 5_000, 3, 3, 1),
                ),
            textFollowAvailable = true,
            totalPcmSampleCount = 100_000,
            followAvailable = true,
        )
}
