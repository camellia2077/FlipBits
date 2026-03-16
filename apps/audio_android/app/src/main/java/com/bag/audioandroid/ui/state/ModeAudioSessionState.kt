package com.bag.audioandroid.ui.state

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioVisualizationTrack
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.UiText

data class ModeAudioSessionState(
    val inputText: String = "",
    val generatedPcm: ShortArray = shortArrayOf(),
    val generatedVisualization: AudioVisualizationTrack? = null,
    val generatedFlashVoicingStyle: FlashVoicingStyleOption? = null,
    val resultText: String = "",
    val statusText: UiText = UiText.Resource(R.string.status_ready_to_encode),
    val playback: PlaybackUiState = PlaybackUiState()
)
