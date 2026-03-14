package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.asString
import com.bag.audioandroid.ui.state.PlaybackUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTabScreen(
    transportMode: TransportModeOption,
    onTransportModeSelected: (TransportModeOption) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    resultText: String,
    statusText: UiText,
    playback: PlaybackUiState,
    playbackSequenceMode: PlaybackSequenceMode,
    playbackSampleCount: Int,
    savedAudioItems: List<SavedAudioItem>,
    showSavedAudioSheet: Boolean,
    onEncode: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipToPreviousTrack: () -> Unit,
    onSkipToNextTrack: () -> Unit,
    onPlaybackSequenceModeSelected: (PlaybackSequenceMode) -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onScrubStarted: () -> Unit,
    onScrubChanged: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onDecode: () -> Unit,
    onClear: () -> Unit,
    onClearResult: () -> Unit,
    onExportAudio: () -> Unit,
    onShareSavedAudio: (() -> Unit)?,
    onOpenSavedAudioSheet: () -> Unit,
    onCloseSavedAudioSheet: () -> Unit,
    onSavedAudioSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedStatusText = statusText.asString()
    val displayedSamples = playback.displayedSamples
    val displayedTime = formatDurationMillis(samplesToMillis(displayedSamples, playback.sampleRateHz))
    val totalTime = formatDurationMillis(samplesToMillis(playback.totalSamples, playback.sampleRateHz))
    val scrollState = rememberScrollState()
    var inputExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }
    var resultExpanded by rememberSaveable(transportMode) { mutableStateOf(true) }
    var savedAudioFilter by remember(transportMode, showSavedAudioSheet) {
        mutableStateOf(SavedAudioModeFilter.fromTransportMode(transportMode))
    }

    if (showSavedAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = onCloseSavedAudioSheet
        ) {
            SavedAudioPickerSheet(
                savedAudioItems = savedAudioItems,
                selectedFilter = savedAudioFilter,
                onFilterSelected = { savedAudioFilter = it },
                onSavedAudioSelected = onSavedAudioSelected
            )
        }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.audio_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        AudioModeSwitcher(
            transportMode = transportMode,
            onTransportModeSelected = onTransportModeSelected,
            modifier = Modifier.fillMaxWidth()
        )
        AudioPlaybackCard(
            statusText = resolvedStatusText,
            playbackSampleCount = playbackSampleCount,
            displayedSamples = displayedSamples,
            totalSamples = playback.totalSamples,
            isScrubbing = playback.isScrubbing,
            displayedTime = displayedTime,
            totalTime = totalTime,
            isPlaying = playback.isPlaying,
            playbackSequenceMode = playbackSequenceMode,
            onTogglePlayback = onTogglePlayback,
            onSkipToPreviousTrack = onSkipToPreviousTrack,
            onSkipToNextTrack = onSkipToNextTrack,
            onPlaybackSequenceModeSelected = onPlaybackSequenceModeSelected,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            onExportAudio = onExportAudio,
            onShareSavedAudio = onShareSavedAudio,
            onOpenSavedAudioSheet = onOpenSavedAudioSheet,
            onScrubStarted = onScrubStarted,
            onScrubChanged = onScrubChanged,
            onScrubFinished = onScrubFinished
        )
        AudioInputActionsCard(
            transportMode = transportMode,
            inputText = inputText,
            onInputTextChange = onInputTextChange,
            onEncode = onEncode,
            expanded = inputExpanded,
            onToggleExpanded = { inputExpanded = !inputExpanded }
        )
        AudioResultCard(
            resultText = resultText,
            expanded = resultExpanded,
            onToggleExpanded = { resultExpanded = !resultExpanded },
            onDecode = onDecode,
            onClearInput = onClear,
            onClearResult = onClearResult
        )
    }
}
