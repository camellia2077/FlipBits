package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AudioSessionDecodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    workerDispatcher: CoroutineDispatcher,
) {
    private val requestFactory = DecodeRequestFactory(sampleRateHz = sampleRateHz, frameSamples = frameSamples)
    private val stateReducer =
        DecodeStateReducer(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
        )
    private val decodeRunner =
        DecodeRunner(
            audioCodecGateway = audioCodecGateway,
            workerDispatcher = workerDispatcher,
        )

    fun onDecode() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            return
        }
        when (val source = current.currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> onDecodeGenerated(current, source.mode)
            is AudioPlaybackSource.Saved -> onDecodeSaved(current, source.itemId)
        }
    }

    fun ensureCurrentPlaybackDecodedForLyrics() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            return
        }
        val source = current.currentPlaybackSource as? AudioPlaybackSource.Saved ?: return
        val selectedSavedAudio =
            current.selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?: return
        val alreadyDecodedForLyrics =
            selectedSavedAudio.decodedPayload.textDecodeStatusCode !=
                BagDecodeContentCodes.STATUS_UNAVAILABLE ||
                selectedSavedAudio.followData.textFollowAvailable
        if (alreadyDecodedForLyrics) {
            return
        }
        onDecodeSaved(current, source.itemId)
    }

    private fun onDecodeGenerated(
        current: AudioAppUiState,
        mode: TransportModeOption,
    ) {
        val session = current.sessions.getValue(mode)
        if (session.generatedPcm.isEmpty()) {
            stateReducer.applyNoGeneratedAudio(mode)
            return
        }
        val request =
            requestFactory.buildGenerated(
                current = current,
                mode = mode,
                generatedPcm = session.generatedPcm,
                metadata = session.generatedAudioMetadata,
            )
        launchGeneratedDecode(request)
    }

    private fun onDecodeSaved(
        current: AudioAppUiState,
        itemId: String,
    ) {
        val selectedSavedAudio =
            current.selectedSavedAudio
                ?.takeIf { it.item.itemId == itemId }
                ?: return
        val mode = TransportModeOption.fromWireName(selectedSavedAudio.item.modeWireName)
        if (mode == null) {
            stateReducer.applySavedLoadFailure()
            return
        }
        val request = requestFactory.buildSaved(current, mode, selectedSavedAudio)
        launchSavedDecode(itemId, request)
    }

    private fun launchGeneratedDecode(request: DecodeRequest) {
        stateReducer.markBusy(request.mode)
        scope.launch {
            stateReducer.reduceGeneratedResult(
                request.mode,
                decodeRunner.execute(request),
            )
        }
    }

    private fun launchSavedDecode(
        itemId: String,
        request: DecodeRequest,
    ) {
        stateReducer.markBusy(request.mode)
        scope.launch {
            stateReducer.reduceSavedResult(
                itemId = itemId,
                mode = request.mode,
                result = decodeRunner.execute(request),
            )
        }
    }
}

private class DecodeRequestFactory(
    private val sampleRateHz: Int,
    private val frameSamples: Int,
) {
    fun buildGenerated(
        current: AudioAppUiState,
        mode: TransportModeOption,
        generatedPcm: ShortArray,
        metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
    ): DecodeRequest =
        DecodeRequest(
            mode = mode,
            generatedPcm = generatedPcm,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            flashPreset =
                if (mode == TransportModeOption.Flash) {
                    current.sessions.getValue(mode).generatedFlashVoicingStyle ?: current.selectedFlashVoicingStyle
                } else {
                    FlashVoicingStyleOption.CodedBurst
                },
            segmentedPcm = metadata?.segmentSampleCounts?.let { splitPcmIntoSegments(generatedPcm, it) },
        )

    fun buildSaved(
        current: AudioAppUiState,
        mode: TransportModeOption,
        savedAudio: SavedAudioPlaybackSelection,
    ): DecodeRequest =
        DecodeRequest(
            mode = mode,
            generatedPcm = savedAudio.pcm,
            sampleRateHz = savedAudio.sampleRateHz,
            frameSamples = savedAudio.metadata?.frameSamples ?: frameSamples,
            flashPreset =
                if (savedAudio.item.modeWireName == TransportModeOption.Flash.wireName) {
                    savedAudio.item.flashVoicingStyle ?: current.selectedFlashVoicingStyle
                } else {
                    FlashVoicingStyleOption.CodedBurst
                },
            segmentedPcm =
                savedAudio.metadata?.segmentSampleCounts?.let {
                    splitPcmIntoSegments(savedAudio.pcm, it)
                },
        )
}

private class DecodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val workerDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(request: DecodeRequest): DecodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val validationIssue =
                audioCodecGateway.validateDecodeConfig(
                    request.sampleRateHz,
                    request.frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
                )
            if (validationIssue != BagApiCodes.VALIDATION_OK) {
                return@withContext DecodeResult.ValidationFailure(validationIssue)
            }

            val decoded =
                request.segmentedPcm?.let { segmentedPcm ->
                    // Decode each stored segment independently, then merge the
                    // user-facing payload/follow views back into one long-text result.
                    mergeSegmentedDecodedPayloadResults(
                        segmentedPcm.map { segmentPcm ->
                            audioCodecGateway.decodeGeneratedPcm(
                                segmentPcm,
                                request.sampleRateHz,
                                request.frameSamples,
                                request.mode.nativeValue,
                                request.flashPreset.signalProfileValue,
                                request.flashPreset.voicingFlavorValue,
                            )
                        },
                    )
                }
                    ?: audioCodecGateway.decodeGeneratedPcm(
                        request.generatedPcm,
                        request.sampleRateHz,
                        request.frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                    )

            DecodeResult.Success(decoded)
        }
}

private class DecodeStateReducer(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
) {
    fun applyNoGeneratedAudio(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(statusText = UiText.Resource(R.string.status_no_audio_for_mode))
        }
    }

    fun applySavedLoadFailure() {
        sessionStateStore.updateCurrentSession {
            it.copy(statusText = UiText.Resource(R.string.status_saved_audio_load_failed))
        }
    }
    fun markBusy(mode: TransportModeOption) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                isCodecBusy = true,
                encodeProgress = null,
                encodePhase = null,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_decoding,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    fun reduceGeneratedResult(
        mode: TransportModeOption,
        result: DecodeResult,
    ) {
        when (result) {
            is DecodeResult.ValidationFailure -> applyValidationFailure(result.validationIssue)
            is DecodeResult.Success -> applyGeneratedSuccess(mode, result.decoded)
        }
    }

    fun reduceSavedResult(
        itemId: String,
        mode: TransportModeOption,
        result: DecodeResult,
    ) {
        when (result) {
            is DecodeResult.ValidationFailure -> applyValidationFailure(result.validationIssue)
            is DecodeResult.Success -> {
                val status = decodeStatusText(mode, result.decoded.decodedPayload)
                applySavedSuccess(itemId, result.decoded, status)
            }
        }
    }

    private fun applyGeneratedSuccess(
        mode: TransportModeOption,
        decoded: DecodedAudioPayloadResult,
    ) {
        val status = decodeStatusText(mode, decoded.decodedPayload)
        sessionStateStore.updateSession(mode) {
            it.copy(
                decodedPayload = decoded.decodedPayload,
                followData = decoded.followData,
                statusText = status,
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
            )
        }
    }

    private fun applySavedSuccess(
        itemId: String,
        decoded: DecodedAudioPayloadResult,
        status: UiText,
    ) {
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        decodedPayload = decoded.decodedPayload,
                        followData = decoded.followData,
                    ),
            )
        }
        applyIdleStatus(status)
    }

    private fun applyValidationFailure(validationIssue: Int) {
        applyIdleStatus(uiTextMapper.validationIssue(validationIssue))
    }

    private fun applyIdleStatus(statusText: UiText) {
        sessionStateStore.updateCurrentSession {
            it.copy(
                statusText = statusText,
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
            )
        }
    }

    private fun decodeStatusText(
        mode: TransportModeOption,
        decodedPayload: DecodedPayloadViewData,
    ): UiText =
        if (
            decodedPayload.textDecodeStatusCode == BagDecodeContentCodes.STATUS_INTERNAL_ERROR ||
            (!decodedPayload.rawPayloadAvailable && !decodedPayload.hasTextResult)
        ) {
            uiTextMapper.errorCode(BagApiCodes.ERROR_INTERNAL)
        } else {
            UiText.Resource(
                R.string.status_mode_decode_completed,
                listOf(mode.wireName),
            )
        }
}

private data class DecodeRequest(
    val mode: TransportModeOption,
    val generatedPcm: ShortArray,
    val sampleRateHz: Int,
    val frameSamples: Int,
    val flashPreset: FlashVoicingStyleOption,
    val segmentedPcm: List<ShortArray>? = null,
)

private sealed interface DecodeResult {
    data class ValidationFailure(
        val validationIssue: Int,
    ) : DecodeResult

    data class Success(
        val decoded: DecodedAudioPayloadResult,
    ) : DecodeResult
}
