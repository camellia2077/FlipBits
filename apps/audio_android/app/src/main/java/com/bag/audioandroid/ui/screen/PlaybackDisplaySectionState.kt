package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

internal data class PlaybackDisplaySectionState(
    val playbackDisplayMode: PlaybackDisplayMode,
    val flashVisualizationModeName: String,
    val morseVisualizationModeName: String,
    val lyricsExpanded: Boolean,
    val onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    val onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
    val onMorseVisualizationModeSelected: (MiniMorseVisualizationMode) -> Unit,
    val onLyricsExpandedChanged: (Boolean) -> Unit,
)

@Composable
internal fun rememberPlaybackDisplaySectionState(
    isFlashMode: Boolean,
    onLyricsRequested: () -> Unit,
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Lyrics,
    initialFlashVisualizationMode: FlashSignalVisualizationMode = FlashSignalVisualizationMode.Lanes,
    initialMorseVisualizationMode: MiniMorseVisualizationMode = MiniMorseVisualizationMode.Horizontal,
    onDisplayModeSelected: (PlaybackDisplayMode) -> Unit = {},
): PlaybackDisplaySectionState {
    var playbackDisplayModeName by rememberSaveable { mutableStateOf(initialDisplayMode.name) }
    var flashVisualizationModeName by rememberSaveable(isFlashMode, initialFlashVisualizationMode) {
        mutableStateOf(initialFlashVisualizationMode.name)
    }
    var morseVisualizationModeName by rememberSaveable(initialMorseVisualizationMode) {
        mutableStateOf(initialMorseVisualizationMode.name)
    }
    var lyricsExpanded by rememberSaveable { mutableStateOf(false) }
    val playbackDisplayMode =
        remember(playbackDisplayModeName) {
            PlaybackDisplayMode.entries.firstOrNull { it.name == playbackDisplayModeName } ?: PlaybackDisplayMode.Lyrics
        }

    return remember(
        playbackDisplayMode,
        flashVisualizationModeName,
        morseVisualizationModeName,
        lyricsExpanded,
        onLyricsRequested,
        onDisplayModeSelected,
    ) {
        PlaybackDisplaySectionState(
            playbackDisplayMode = playbackDisplayMode,
            flashVisualizationModeName = flashVisualizationModeName,
            morseVisualizationModeName = morseVisualizationModeName,
            lyricsExpanded = lyricsExpanded,
            onDisplayModeSelected = { option ->
                playbackDisplayModeName = option.name
                onDisplayModeSelected(option)
                if (option == PlaybackDisplayMode.Lyrics) {
                    onLyricsRequested()
                }
            },
            onFlashVisualizationModeSelected = { selectedMode ->
                flashVisualizationModeName = selectedMode.name
            },
            onMorseVisualizationModeSelected = { selectedMode ->
                morseVisualizationModeName = selectedMode.name
            },
            onLyricsExpandedChanged = { expanded ->
                lyricsExpanded = expanded
            },
        )
    }
}
