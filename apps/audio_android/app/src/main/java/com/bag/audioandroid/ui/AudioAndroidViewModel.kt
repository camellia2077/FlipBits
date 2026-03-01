package com.bag.audioandroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.audio.playPcm
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioAndroidViewModel(
    private val audioCodecGateway: AudioCodecGateway
) : ViewModel() {
    private val _uiState = MutableStateFlow(AudioAppUiState())
    val uiState: StateFlow<AudioAppUiState> = _uiState

    init {
        val coreVersion = audioCodecGateway.getCoreVersion().ifBlank { "unknown" }
        _uiState.update {
            it.copy(
                presentationVersion = BuildConfig.VERSION_NAME.ifBlank { "unknown" },
                coreVersion = coreVersion
            )
        }
    }

    fun onTabSelected(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onOpenAboutPage() {
        _uiState.update { it.copy(showAboutPage = true) }
    }

    fun onCloseAboutPage() {
        _uiState.update { it.copy(showAboutPage = false) }
    }

    fun onOpenLicensesPage() {
        _uiState.update { it.copy(showLicensesPage = true, showAboutPage = false) }
    }

    fun onCloseLicensesPage() {
        _uiState.update { it.copy(showLicensesPage = false, showAboutPage = true) }
    }

    fun onPaletteSelected(palette: PaletteOption) {
        _uiState.update { it.copy(selectedPalette = palette) }
    }

    fun onInputTextChange(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun onEncode() {
        val current = _uiState.value
        val pcm = audioCodecGateway.encodeTextToPcm(current.inputText, SAMPLE_RATE_HZ, FRAME_SAMPLES)
        val status = if (pcm.isEmpty()) {
            "音频生成失败或输入为空"
        } else {
            "音频已生成，样本数=${pcm.size}"
        }
        _uiState.update {
            it.copy(
                generatedPcm = pcm,
                statusText = status,
                isPlaying = false,
                playbackProgress = 0f
            )
        }
    }

    fun onPlay() {
        val current = _uiState.value
        if (current.generatedPcm.isEmpty()) {
            _uiState.update { it.copy(statusText = "请先生成音频") }
            return
        }
        if (current.isPlaying) {
            _uiState.update { it.copy(statusText = "音频正在播放") }
            return
        }

        _uiState.update {
            it.copy(
                statusText = "正在播放生成音频",
                isPlaying = true,
                playbackProgress = 0f
            )
        }
        val pcmCopy = current.generatedPcm.copyOf()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playPcm(pcmCopy, SAMPLE_RATE_HZ) { progress ->
                    _uiState.update { state ->
                        state.copy(playbackProgress = progress.coerceIn(0f, 1f))
                    }
                }
                _uiState.update {
                    it.copy(
                        statusText = "播放完成",
                        isPlaying = false,
                        playbackProgress = 1f
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(statusText = "播放失败", isPlaying = false) }
            }
        }
    }

    fun onDecode() {
        val current = _uiState.value
        if (current.generatedPcm.isEmpty()) {
            _uiState.update { it.copy(statusText = "请先生成音频") }
            return
        }

        val decoded = audioCodecGateway.decodeGeneratedPcm(
            current.generatedPcm,
            SAMPLE_RATE_HZ,
            FRAME_SAMPLES
        )
        _uiState.update { it.copy(resultText = decoded, statusText = "解析完成") }
    }

    fun onClear() {
        _uiState.update {
            it.copy(
                inputText = "",
                generatedPcm = shortArrayOf(),
                resultText = "",
                statusText = "已清空",
                isPlaying = false,
                playbackProgress = 0f
            )
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44100
        const val FRAME_SAMPLES = 2205
    }
}
