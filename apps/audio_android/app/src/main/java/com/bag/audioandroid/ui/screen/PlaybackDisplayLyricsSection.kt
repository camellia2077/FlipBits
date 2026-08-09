package com.bag.audioandroid.ui.screen

import android.util.Log
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.ui.model.TransportModeOption
import kotlinx.coroutines.launch

@Composable
internal fun ExpandablePlaybackLyricsSection(
    followData: PayloadFollowViewData,
    displayedSamples: Int,
    sampleRateHz: Int,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    transportMode: TransportModeOption?,
    playbackDisplayMode: PlaybackDisplayMode,
    lyricsExpanded: Boolean,
    tokenStripHeightDp: Float?,
    extraLyricsRecoveryHeight: Dp,
    applyLyricsPreviewBonusLine: Boolean,
    onOpenLyricsNavigator: () -> Unit,
    onLyricsExpandedChanged: (Boolean) -> Unit,
    onSeekToSample: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsLayoutModel =
        rememberPlaybackLyricsLayoutModel(
            followData = followData,
            displayedSamples = displayedSamples,
            transportMode = transportMode,
            playbackDisplayMode = playbackDisplayMode,
            tokenStripHeightDp = tokenStripHeightDp,
            extraLyricsRecoveryHeight = extraLyricsRecoveryHeight,
            applyLyricsPreviewBonusLine = applyLyricsPreviewBonusLine,
            lyricsExpanded = true,
        )
    val selectionListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex =
                (lyricsLayoutModel.activeLineIndex).coerceIn(
                    minimumValue = 0,
                    maximumValue = lyricsLayoutModel.displayLineRanges.lastIndex.coerceAtLeast(0),
                ),
        )
    var isSelectingLyricsLine by remember { mutableStateOf(false) }
    var selectedLineStartSample by remember { mutableStateOf<Int?>(null) }
    val latestSelectedLineStartSample by rememberUpdatedState(selectedLineStartSample)
    val latestOnSeekToSample by rememberUpdatedState(onSeekToSample)
    val lyricsSelectionScope = rememberCoroutineScope()
    LaunchedEffect(lyricsLayoutModel.activeLineIndex, isSelectingLyricsLine, lyricsLayoutModel.displayLineRanges.size) {
        if (!isSelectingLyricsLine && lyricsLayoutModel.activeLineIndex >= 0) {
            selectionListState.scrollToItem(lyricsLayoutModel.activeLineIndex)
        }
    }
    SideEffect {
        debugPlayerLyricsCapacity(
            "lyricsLayout",
            "transport=${transportMode?.wireName ?: "unknown"} " +
                "displayMode=${playbackDisplayMode.name.lowercase()} selectable=true " +
                "tokenStripHeightDp=${tokenStripHeightDp ?: -1f} " +
                "extraRecoveryDp=${extraLyricsRecoveryHeight.value} " +
                "effectiveExtraRecoveryDp=${lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight.value} " +
                "bonusLine=$applyLyricsPreviewBonusLine " +
                "compactVisibleLineCount=${lyricsLayoutModel.compactVisibleLineCount} " +
                "displayLineCount=${lyricsLayoutModel.displayLineRanges.size} " +
                "prefersWrapped=${lyricsLayoutModel.prefersWrappedLines}",
        )
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("playback-lyrics-section"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .pointerInput(lyricsLayoutModel.displayLineRanges) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isSelectingLyricsLine = true
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                lyricsSelectionScope.launch {
                                    selectionListState.scrollBy(-dragAmount)
                                }
                            },
                            onDragEnd = {
                                latestSelectedLineStartSample?.let(latestOnSeekToSample)
                                isSelectingLyricsLine = false
                            },
                            onDragCancel = {
                                isSelectingLyricsLine = false
                            },
                        )
                    },
        ) {
            if (isSelectingLyricsLine) {
                PlaybackLyricsFullList(
                    followData = followData,
                    displayLineRanges = lyricsLayoutModel.displayLineRanges,
                    activeLineIndex = lyricsLayoutModel.activeLineIndex,
                    sampleRateHz = sampleRateHz,
                    onSeekToSample = onSeekToSample,
                    extraHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    listState = selectionListState,
                    autoSeekOnScrollStop = false,
                    selectionFadeInactiveLines = true,
                    onSelectedSampleChanged = { selectedLineStartSample = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PlaybackTokenContextTape(
                    followData = followData,
                    displayedSamples = displayedSamples,
                    isPlaying = isPlaying,
                    isScrubbing = isScrubbing,
                    visibleLineCount = lyricsLayoutModel.compactVisibleLineCount,
                    extraContainerHeight = lyricsLayoutModel.effectiveExtraLyricsRecoveryHeight,
                    layoutMeasurementSource =
                        when (playbackDisplayMode) {
                            PlaybackDisplayMode.Visual -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Mix -> PlaybackLyricsLayoutMeasurementSource.VisualPreview
                            PlaybackDisplayMode.Lyrics -> PlaybackLyricsLayoutMeasurementSource.LyricsPreview
                        },
                    onSeekToSample = onSeekToSample,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (followData.textFollowAvailable && followData.textTokens.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onOpenLyricsNavigator,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .size(36.dp)
                        .testTag("playback-lyrics-open-navigator")
                        .semantics {
                            contentDescription = "Open full lyrics"
                        },
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = stringResource(R.string.audio_lyrics_navigator_open),
                )
            }
        }
    }
}

private fun debugPlayerLyricsCapacity(
    label: String,
    message: String,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    try {
        Log.d("PlayerLyricsCapacity", "$label $message")
    } catch (_: RuntimeException) {
    }
}
