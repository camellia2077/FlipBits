package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioEncodePhase
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class AudioSessionEncodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val stopPlayback: () -> Unit,
    workerDispatcher: CoroutineDispatcher,
) {
    private val requestFactory = EncodeRequestFactory()
    private val stateReducer =
        EncodeStateReducer(
            uiState = uiState,
            sessionStateStore = sessionStateStore,
            uiTextMapper = uiTextMapper,
            playbackRuntimeGateway = playbackRuntimeGateway,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
        )
    private val encodeRunner =
        EncodeRunner(
            audioCodecGateway = audioCodecGateway,
            sampleRateHz = sampleRateHz,
            frameSamples = frameSamples,
            workerDispatcher = workerDispatcher,
            onProgress = stateReducer::applyProgress,
        )

    private var encodeJob: Job? = null

    fun onEncode() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            return
        }

        val request = requestFactory.build(current)
        stopPlayback()
        stateReducer.markBusy(request)
        launchEncode(request)
    }

    fun onCancelEncode() {
        val mode = uiState.value.transportMode
        val session = uiState.value.sessions.getValue(mode)
        if (!session.isCodecBusy || session.encodeProgress == null || session.isEncodeCancelling) {
            return
        }

        stateReducer.markCancelling(mode)
        encodeJob?.cancel()
    }

    private fun launchEncode(request: EncodeRequest) {
        encodeJob =
            scope.launch {
                val runningJob = currentCoroutineContext()[Job]
                try {
                    stateReducer.reduceResult(request, encodeRunner.execute(request))
                } catch (cancelled: CancellationException) {
                    handleEncodeCancellation(request.mode, cancelled)
                } finally {
                    clearEncodeJob(runningJob)
                }
            }
    }

    private fun handleEncodeCancellation(
        mode: TransportModeOption,
        cancelled: CancellationException,
    ) {
        val sessionAfterCancel = uiState.value.sessions.getValue(mode)
        if (sessionAfterCancel.isEncodeCancelling) {
            stateReducer.applyCancelled(mode)
        } else {
            throw cancelled
        }
    }

    private fun clearEncodeJob(runningJob: Job?) {
        if (encodeJob === runningJob) {
            encodeJob = null
        }
    }
}

private class EncodeRequestFactory {
    fun build(current: AudioAppUiState): EncodeRequest =
        EncodeRequest(
            mode = current.transportMode,
            inputText = current.currentSession.inputText,
            selectedFlashVoicingStyle = current.selectedFlashVoicingStyle,
            flashPreset =
                if (current.transportMode == TransportModeOption.Flash) {
                    current.selectedFlashVoicingStyle
                } else {
                    FlashVoicingStyleOption.CodedBurst
                },
            appVersion = current.presentationVersion.ifBlank { "unknown" },
            coreVersion = current.coreVersion.ifBlank { "unknown" },
        )
}

private class EncodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher,
    private val onProgress: (TransportModeOption, EncodeProgressUpdate) -> Unit,
) {
    suspend fun execute(request: EncodeRequest): EncodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            val validationIssue =
                audioCodecGateway.validateEncodeRequest(
                    request.inputText,
                    sampleRateHz,
                    frameSamples,
                    request.mode.nativeValue,
                    request.flashPreset.signalProfileValue,
                    request.flashPreset.voicingFlavorValue,
                )
            if (validationIssue != BagApiCodes.VALIDATION_OK) {
                return@withContext EncodeResult.ValidationFailure(validationIssue)
            }

            when (
                val gatewayResult =
                    audioCodecGateway.encodeTextToPcm(
                        request.inputText,
                        sampleRateHz,
                        frameSamples,
                        request.mode.nativeValue,
                        request.flashPreset.signalProfileValue,
                        request.flashPreset.voicingFlavorValue,
                        onProgress = { update -> onProgress(request.mode, update) },
                    )
            ) {
                is EncodeAudioResult.Success ->
                    EncodeResult.Success(
                        pcm = gatewayResult.pcm,
                        followData = gatewayResult.followData,
                    )
                EncodeAudioResult.Cancelled -> EncodeResult.Cancelled
                is EncodeAudioResult.Failed -> EncodeResult.Failure(gatewayResult.errorCode)
            }
        }
}

private class EncodeStateReducer(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val sessionStateStore: AudioSessionStateStore,
    private val uiTextMapper: BagUiTextMapper,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
) {
    fun markBusy(request: EncodeRequest) {
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                isCodecBusy = true,
                encodeProgress = 0f,
                encodePhase = AudioEncodePhase.PreparingInput,
                isEncodeCancelling = false,
                statusText =
                    uiTextMapper.encodePhaseStatus(
                        request.mode.wireName,
                        AudioEncodePhase.PreparingInput,
                    ),
            )
        }
    }

    fun markCancelling(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                isEncodeCancelling = true,
                encodePhase = it.encodePhase,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_canceling,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    fun applyProgress(
        mode: TransportModeOption,
        update: EncodeProgressUpdate,
    ) {
        val clampedProgress = update.progress0To1.coerceIn(0f, 1f)
        sessionStateStore.updateSession(mode) { session ->
            if (session.isEncodeCancelling) {
                return@updateSession session
            }
            if (session.encodePhase == update.phase) {
                session.copy(encodeProgress = clampedProgress)
            } else {
                session.copy(
                    encodeProgress = clampedProgress,
                    encodePhase = update.phase,
                    statusText = uiTextMapper.encodePhaseStatus(mode.wireName, update.phase),
                )
            }
        }
    }

    fun reduceResult(
        request: EncodeRequest,
        result: EncodeResult,
    ) {
        when (result) {
            is EncodeResult.ValidationFailure -> applyValidationFailure(request.mode, result.validationIssue)
            EncodeResult.Cancelled -> applyCancelled(request.mode)
            is EncodeResult.Failure -> applyFailure(request.mode, result.errorCode)
            is EncodeResult.Success -> applySuccess(request, result)
        }
    }

    fun applyCancelled(mode: TransportModeOption) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_cancelled,
                        listOf(mode.wireName),
                    ),
            )
        }
    }

    private fun applyValidationFailure(
        mode: TransportModeOption,
        validationIssue: Int,
    ) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                generatedPcm = shortArrayOf(),
                generatedAudioMetadata = null,
                generatedFlashVoicingStyle = null,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                statusText = uiTextMapper.validationIssue(validationIssue),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.cleared(),
            )
        }
    }

    private fun applyFailure(
        mode: TransportModeOption,
        errorCode: Int,
    ) {
        sessionStateStore.updateSession(mode) {
            it.copy(
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = PayloadFollowViewData.Empty,
                statusText = uiTextMapper.errorCode(errorCode),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.cleared(),
            )
        }
    }

    private fun applySuccess(
        request: EncodeRequest,
        result: EncodeResult.Success,
    ) {
        val pcm = result.pcm
        val generatedFlashStyle =
            if (request.mode == TransportModeOption.Flash && pcm.isNotEmpty()) {
                request.selectedFlashVoicingStyle
            } else {
                null
            }
        sessionStateStore.updateSession(request.mode) {
            it.copy(
                generatedPcm = pcm,
                generatedAudioMetadata =
                    GeneratedAudioMetadata(
                        mode = request.mode,
                        flashVoicingStyle = generatedFlashStyle,
                        createdAtIsoUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        durationMs = (pcm.size.toLong() * 1000L) / sampleRateHz.toLong(),
                        frameSamples = frameSamples,
                        pcmSampleCount = pcm.size,
                        appVersion = request.appVersion,
                        coreVersion = request.coreVersion,
                    ),
                generatedFlashVoicingStyle = generatedFlashStyle,
                decodedPayload = DecodedPayloadViewData.Empty,
                followData = result.followData,
                statusText =
                    UiText.Resource(
                        R.string.status_mode_audio_generated,
                        listOf(request.mode.wireName, pcm.size),
                    ),
                isCodecBusy = false,
                encodeProgress = null,
                encodePhase = null,
                isEncodeCancelling = false,
                playback = playbackRuntimeGateway.load(pcm.size, sampleRateHz),
            )
        }
        if (uiState.value.transportMode == request.mode) {
            uiState.update {
                it.copy(currentPlaybackSource = AudioPlaybackSource.Generated(request.mode))
            }
        }
    }
}

private data class EncodeRequest(
    val mode: TransportModeOption,
    val inputText: String,
    val selectedFlashVoicingStyle: FlashVoicingStyleOption,
    val flashPreset: FlashVoicingStyleOption,
    val appVersion: String,
    val coreVersion: String,
)

private sealed interface EncodeResult {
    data class ValidationFailure(
        val validationIssue: Int,
    ) : EncodeResult

    data object Cancelled : EncodeResult

    data class Failure(
        val errorCode: Int,
    ) : EncodeResult

    data class Success(
        val pcm: ShortArray,
        val followData: PayloadFollowViewData,
    ) : EncodeResult
}
