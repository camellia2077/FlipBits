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
    val onDisplayModeSelected: (PlaybackDisplayMode) -> Unit,
    val onFlashVisualizationModeSelected: (FlashSignalVisualizationMode) -> Unit,
)

@Composable
internal fun rememberPlaybackDisplaySectionState(
    isFlashMode: Boolean,
    onLyricsRequested: () -> Unit,
    initialDisplayMode: PlaybackDisplayMode = PlaybackDisplayMode.Visual,
): PlaybackDisplaySectionState {
    var playbackDisplayModeName by rememberSaveable { mutableStateOf(initialDisplayMode.name) }
    var flashVisualizationModeName by rememberSaveable(isFlashMode) {
        mutableStateOf(FlashSignalVisualizationMode.ToneTracks.name)
    }
    val playbackDisplayMode =
        remember(playbackDisplayModeName) {
            PlaybackDisplayMode.entries.firstOrNull { it.name == playbackDisplayModeName } ?: PlaybackDisplayMode.Visual
        }

    return remember(
        playbackDisplayMode,
        flashVisualizationModeName,
        onLyricsRequested,
    ) {
        PlaybackDisplaySectionState(
            playbackDisplayMode = playbackDisplayMode,
            flashVisualizationModeName = flashVisualizationModeName,
            onDisplayModeSelected = { option ->
                playbackDisplayModeName = option.name
                if (option == PlaybackDisplayMode.Lyrics) {
                    onLyricsRequested()
                }
            },
            onFlashVisualizationModeSelected = { selectedMode ->
                flashVisualizationModeName = selectedMode.name
            },
        )
    }
}
