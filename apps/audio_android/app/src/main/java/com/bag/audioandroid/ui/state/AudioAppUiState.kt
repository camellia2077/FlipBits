package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.theme.MaterialPalettes

data class AudioAppUiState(
    val selectedTab: AppTab = AppTab.Audio,
    val selectedLanguage: AppLanguageOption = AppLanguageOption.FollowSystem,
    val showAboutPage: Boolean = false,
    val showLicensesPage: Boolean = false,
    val presentationVersion: String = "",
    val coreVersion: String = "",
    val selectedPalette: PaletteOption = MaterialPalettes.first(),
    val transportMode: TransportModeOption = TransportModeOption.Flash,
    val sessions: Map<TransportModeOption, ModeAudioSessionState> = defaultModeSessions(),
    val currentPlaybackSource: AudioPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
    val playbackSequenceMode: PlaybackSequenceMode = PlaybackSequenceMode.Normal,
    val selectedSavedAudio: SavedAudioPlaybackSelection? = null,
    val savedAudioItems: List<SavedAudioItem> = emptyList(),
    val librarySelection: LibrarySelectionUiState = LibrarySelectionUiState(),
    val showSavedAudioSheet: Boolean = false,
    val libraryStatusText: UiText = UiText.Empty
) {
    val currentSession: ModeAudioSessionState
        get() = sessions.getValue(transportMode)

    val currentPlayback: PlaybackUiState
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).playback
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.playback
                ?: PlaybackUiState()
        }

    val currentPlaybackSampleCount: Int
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> sessions.getValue(source.mode).generatedPcm.size
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.pcm
                ?.size
                ?: 0
        }

    val currentSavedAudioItem: SavedAudioItem?
        get() = when (val source = currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> null
            is AudioPlaybackSource.Saved -> selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?.item
        }

    val canSkipPrevious: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex > 0
        }

    val canSkipNext: Boolean
        get() {
            val currentItemId = currentSavedAudioItem?.itemId ?: return false
            val currentIndex = savedAudioItems.indexOfFirst { it.itemId == currentItemId }
            return currentIndex >= 0 && currentIndex < savedAudioItems.lastIndex
        }
}

private fun defaultModeSessions(): Map<TransportModeOption, ModeAudioSessionState> =
    TransportModeOption.entries.associateWith { ModeAudioSessionState() }
