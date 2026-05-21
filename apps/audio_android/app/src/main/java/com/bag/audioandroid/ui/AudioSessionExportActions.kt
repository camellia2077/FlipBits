package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.SnackbarMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AudioSessionExportActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val sessionStateStore: AudioSessionStateStore,
    private val savedAudioRepository: SavedAudioRepository,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val sampleRateHz: Int,
    private val refreshSavedAudioItems: () -> Unit,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun onExportAudio() {
        scope.launch {
            when (val result = exportCurrentGeneratedAudio()) {
                is AudioExportResult.Success ->
                    handleGeneratedAudioExportSuccess(
                        result = result,
                        snackbarMessage = UiText.Resource(R.string.snackbar_audio_saved_to_library),
                    )

                AudioExportResult.Failed ->
                    sessionStateStore
                        .updateCurrentSession {
                            it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
                        }.also {
                            emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
                        }
            }
        }
    }

    fun onShareCurrentGeneratedAudio() {
        scope.launch {
            when (val result = exportCurrentGeneratedAudio()) {
                is AudioExportResult.Success -> {
                    refreshSavedAudioItems()
                    emitSnackbar(UiText.Resource(R.string.snackbar_audio_saved_to_library))
                    if (!savedAudioRepository.shareAudio(result.displayName, result.uriString)) {
                        emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
                    }
                }

                AudioExportResult.Failed ->
                    sessionStateStore
                        .updateCurrentSession {
                            it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
                        }.also {
                            emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
                        }
            }
        }
    }

    private suspend fun exportCurrentGeneratedAudio(): AudioExportResult {
        val current = uiState.value
        val session = current.currentSession
        if (session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
            }
            return AudioExportResult.Failed
        }
        val metadata = session.generatedAudioMetadata
        if (metadata == null) {
            sessionStateStore.updateCurrentSession {
                it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
            }
            return AudioExportResult.Failed
        }
        return withContext(workerDispatcher) {
            savedAudioRepository.exportGeneratedAudio(
                inputText = session.inputText,
                pcm = session.generatedPcm,
                pcmFilePath = session.generatedPcmFilePath,
                sampleRateHz = sampleRateHz,
                metadata = metadata,
            )
        }
    }

    private fun handleGeneratedAudioExportSuccess(
        result: AudioExportResult.Success,
        snackbarMessage: UiText,
    ) {
        val generatedPcmFilePath = uiState.value.currentSession.generatedPcmFilePath
        sessionStateStore
            .updateCurrentSession {
                it.copy(
                    generatedPcmFilePath = null,
                    statusText =
                        UiText.Resource(
                            R.string.status_audio_saved,
                            listOf(result.displayName),
                        ),
                )
            }.also {
                generatedAudioCacheGateway.deleteCachedFile(generatedPcmFilePath)
                generatedAudioCacheGateway.enforceGeneratedAudioCachePolicy(uiState.value)
                emitSnackbar(snackbarMessage)
                refreshSavedAudioItems()
            }
    }

    private fun emitSnackbar(message: UiText) {
        uiState.update { state ->
            state.copy(
                snackbarMessage =
                    SnackbarMessage(
                        id = System.nanoTime(),
                        text = message,
                        durationMillis = EXPORT_SNACKBAR_DURATION_MILLIS,
                    ),
            )
        }
    }

    private companion object {
        const val EXPORT_SNACKBAR_DURATION_MILLIS = 1400L
    }
}
