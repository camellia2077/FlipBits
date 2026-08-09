package com.bag.audioandroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class PlaybackDisplayRenderPolicy(
    val topSpacing: Dp,
    val showsVisualization: Boolean,
    val showsMixSpacer: Boolean,
    val showsFollowSection: Boolean,
    val followContentSpacing: Dp,
    val showsExpandableLyrics: Boolean,
    val bottomSpacing: Dp?,
)

@Composable
internal fun rememberPlaybackDisplayRenderPolicy(playbackDisplayMode: PlaybackDisplayMode): PlaybackDisplayRenderPolicy =
    remember(playbackDisplayMode) {
        when (playbackDisplayMode) {
            PlaybackDisplayMode.Visual ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 10.dp,
                    showsVisualization = true,
                    showsMixSpacer = false,
                    showsFollowSection = false,
                    followContentSpacing = 10.dp,
                    showsExpandableLyrics = true,
                    bottomSpacing = 10.dp,
                )

            PlaybackDisplayMode.Mix ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 10.dp,
                    showsVisualization = true,
                    showsMixSpacer = true,
                    showsFollowSection = true,
                    followContentSpacing = 6.dp,
                    showsExpandableLyrics = false,
                    bottomSpacing = null,
                )

            PlaybackDisplayMode.Lyrics ->
                PlaybackDisplayRenderPolicy(
                    topSpacing = 6.dp,
                    showsVisualization = false,
                    showsMixSpacer = false,
                    showsFollowSection = true,
                    followContentSpacing = 10.dp,
                    showsExpandableLyrics = true,
                    bottomSpacing = 6.dp,
                )
        }
    }
