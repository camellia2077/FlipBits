package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.audio.VoicePreviewPlayer
import com.bag.audioandroid.domain.AudioIoWavCodes
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.VoiceAudioFileGateway
import com.bag.audioandroid.domain.VoiceAudioImportResult
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceFxProcessResult
import com.bag.audioandroid.domain.VoiceFxRecordDiag
import com.bag.audioandroid.domain.VoiceFxStreamCollectedResult
import com.bag.audioandroid.domain.VoiceFxStreamCollector
import com.bag.audioandroid.domain.VoiceLiveConfig
import com.bag.audioandroid.domain.VoiceLiveGateway
import com.bag.audioandroid.domain.VoiceRecordingGateway
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoicePreviewTrackOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.model.defaultPreset
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.AudioDocumentExportSource
import com.bag.audioandroid.ui.state.PendingAudioDocumentExportRequest
import com.bag.audioandroid.ui.state.SnackbarMessage
import com.bag.audioandroid.ui.state.VoiceSessionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LargeClass")
internal class VoiceSessionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val voiceFxGateway: VoiceFxGateway,
    private val voiceRecordingGateway: VoiceRecordingGateway,
    private val voiceLiveGateway: VoiceLiveGateway,
    private val voiceAudioFileGateway: VoiceAudioFileGateway,
    private val voicePreviewPlayer: VoicePreviewPlayer,
    private val savedAudioRepository: SavedAudioRepository? = null,
    private val stopGlobalPlayback: () -> Unit,
    private val onPersistWorkflowModeSelected: (VoiceWorkflowModeOption) -> Unit = {},
    private val onPersistTrackModeSelected: (VoiceTrackModeOption) -> Unit = {},
    private val onPersistInputSourceSelected: (VoiceInputSourceOption) -> Unit = {},
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var recordProcessingCollector: VoiceFxStreamCollector? = null

    fun onRecordPermissionChanged(granted: Boolean) {
        uiState.update { state ->
            state.copy(voiceSession = state.voiceSession.copy(hasRecordPermission = granted))
        }
    }

    fun onWorkflowModeSelected(mode: VoiceWorkflowModeOption) {
        val session = uiState.value.voiceSession
        if (session.selectedWorkflowMode == mode || session.isVoiceTransitionBlocked()) {
            return
        }
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                selectedWorkflowMode = mode,
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        onPersistWorkflowModeSelected(mode)
    }

    fun onTrackModeSelected(mode: VoiceTrackModeOption) {
        val session = uiState.value.voiceSession
        if (session.selectedTrackMode == mode || session.isVoiceTransitionBlocked()) {
            return
        }
        stopLiveInternal()
        stopPreviewInternal()
        updateVoiceSession {
            val nextPreset =
                if (it.selectedPreset.trackMode == mode) {
                    it.selectedPreset
                } else {
                    mode.defaultPreset()
                }
            it.copy(
                selectedTrackMode = mode,
                selectedPreset = nextPreset,
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        onPersistTrackModeSelected(mode)
    }

    fun onInputSourceSelected(source: VoiceInputSourceOption) {
        val session = uiState.value.voiceSession
        if (session.selectedInputSource == source || session.isVoiceTransitionBlocked()) {
            return
        }
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                selectedInputSource = source,
                isRecording = false,
                isLoadingInput = false,
                isProcessing = false,
                inputPcm = shortArrayOf(),
                inputDisplayName = "",
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                sampleRateHz =
                    if (source == VoiceInputSourceOption.Record) {
                        RECORDING_SAMPLE_RATE_HZ
                    } else {
                        it.sampleRateHz
                    },
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        onPersistInputSourceSelected(source)
    }

    fun onRecordProcessingModeSelected(mode: VoiceRecordProcessingModeOption) {
        val session = uiState.value.voiceSession
        if (session.selectedRecordProcessingMode == mode || session.isVoiceTransitionBlocked()) {
            return
        }
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                selectedRecordProcessingMode = mode,
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
    }

    fun onPresetSelected(preset: VoiceFxPresetOption) {
        stopLiveInternal()
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                selectedPreset = preset,
                selectedTrackMode = preset.trackMode,
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
    }

    fun onSubvoiceStyleSelected(subvoiceStyle: VoiceFxSubvoiceStyleOption) {
        stopLiveInternal()
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                selectedSubvoiceStyle = subvoiceStyle,
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
    }

    fun onStartRecording() {
        val session = uiState.value.voiceSession
        if (!session.canStartRecording) {
            return
        }
        stopGlobalPlayback()
        stopPreviewInternal()
        resetRecordProcessingSession()
        VoiceFxRecordDiag.log(
            "startRecording requested workflow=${session.selectedWorkflowMode.id} " +
                "trackMode=${session.selectedTrackMode.id} preset=${session.selectedPreset.id} " +
                "style=${session.selectedSubvoiceStyle.id} sr=${session.sampleRateHz} " +
                "mode=${session.selectedRecordProcessingMode.id} hasPermission=${session.hasRecordPermission}",
        )
        val chunkHandler: ((ShortArray) -> Unit)? =
            if (session.selectedRecordProcessingMode == VoiceRecordProcessingModeOption.WhileRecording) {
                val collector =
                    VoiceFxStreamCollector.create(
                        voiceFxGateway = voiceFxGateway,
                        preset = session.selectedPreset,
                        subvoiceStyle = session.selectedSubvoiceStyle,
                        sampleRateHz = session.sampleRateHz,
                    ) ?: run {
                        VoiceFxRecordDiag.log(
                            "startRecording processor-create-failed preset=${session.selectedPreset.id} " +
                                "style=${session.selectedSubvoiceStyle.id} sr=${session.sampleRateHz}",
                        )
                        updateVoiceSession { it.copy(lastErrorCode = BagApiCodes.ERROR_INTERNAL) }
                        return
                    }
                recordProcessingCollector = collector
                { chunk: ShortArray -> collector.processBlock(chunk) }
            } else {
                null
            }
        if (!voiceRecordingGateway.startRecording(session.sampleRateHz, chunkHandler)) {
            VoiceFxRecordDiag.log(
                "startRecording audioRecord-start-failed preset=${session.selectedPreset.id} " +
                    "style=${session.selectedSubvoiceStyle.id} sr=${session.sampleRateHz}",
            )
            resetRecordProcessingSession()
            updateVoiceSession { it.copy(lastErrorCode = BagApiCodes.ERROR_INTERNAL) }
            return
        }
        updateVoiceSession {
            it.copy(
                isRecording = true,
                isLoadingInput = false,
                isProcessing = false,
                inputPcm = shortArrayOf(),
                inputDisplayName = "",
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
    }

    fun onStopRecording() {
        val session = uiState.value.voiceSession
        if (!session.isRecording) {
            return
        }
        val recorded = voiceRecordingGateway.stopRecording()
        val streamedResult =
            if (session.selectedRecordProcessingMode == VoiceRecordProcessingModeOption.WhileRecording) {
                finishRecordedProcessing()
            } else {
                null
            }
        VoiceFxRecordDiag.log(
            "stopRecording captured preset=${session.selectedPreset.id} style=${session.selectedSubvoiceStyle.id} " +
                "sr=${session.sampleRateHz} mode=${session.selectedRecordProcessingMode.id} input=${recorded.size}",
        )
        val streamedProcessedPcm = streamedResult?.finalMix ?: shortArrayOf()
        val streamedErrorCode = streamedResult?.errorCode ?: BagApiCodes.ERROR_OK
        updateVoiceSession {
            it.copy(
                isRecording = false,
                isLoadingInput = false,
                isProcessing =
                    recorded.isNotEmpty() &&
                        session.selectedRecordProcessingMode == VoiceRecordProcessingModeOption.AfterRecording,
                inputPcm = recorded,
                inputDisplayName = "",
                processedPcm = streamedProcessedPcm,
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                lastErrorCode =
                    when {
                        recorded.isEmpty() -> BagApiCodes.ERROR_INTERNAL
                        session.selectedRecordProcessingMode == VoiceRecordProcessingModeOption.WhileRecording &&
                            streamedProcessedPcm.isEmpty() -> streamedErrorCode
                        else -> BagApiCodes.ERROR_OK
                    },
            )
        }
        if (recorded.isNotEmpty() && session.selectedRecordProcessingMode == VoiceRecordProcessingModeOption.AfterRecording) {
            processRecordedBatch(
                pcm = recorded,
                sampleRateHz = session.sampleRateHz,
                preset = session.selectedPreset,
                subvoiceStyle = session.selectedSubvoiceStyle,
            )
        }
    }

    fun onImportAudio(uriString: String) {
        val session = uiState.value.voiceSession
        if (!session.canImportAudio) {
            return
        }
        stopGlobalPlayback()
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                isLoadingInput = true,
                isRecording = false,
                isProcessing = false,
                inputPcm = shortArrayOf(),
                inputDisplayName = "",
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        scope.launch(workerDispatcher) {
            when (val result = voiceAudioFileGateway.importVoiceAudio(uriString)) {
                is VoiceAudioImportResult.Success ->
                    updateVoiceSession {
                        it.copy(
                            isLoadingInput = false,
                            inputPcm = result.audio.pcm,
                            inputDisplayName = result.audio.displayName,
                            sampleRateHz = result.audio.sampleRateHz,
                            processedPcm = shortArrayOf(),
                            debugMainVoicePcm = shortArrayOf(),
                            debugSubvoicePcm = shortArrayOf(),
                            debugSignalOverlayPcm = shortArrayOf(),
                            isPreviewPlaying = false,
                            lastErrorCode = BagApiCodes.ERROR_OK,
                        )
                    }

                VoiceAudioImportResult.UnsupportedFormat ->
                    updateVoiceSession {
                        it.copy(
                            isLoadingInput = false,
                            inputPcm = shortArrayOf(),
                            inputDisplayName = "",
                            processedPcm = shortArrayOf(),
                            debugMainVoicePcm = shortArrayOf(),
                            debugSubvoicePcm = shortArrayOf(),
                            debugSignalOverlayPcm = shortArrayOf(),
                            isPreviewPlaying = false,
                            lastErrorCode = AudioIoWavCodes.STATUS_UNSUPPORTED_FORMAT,
                        )
                    }

                VoiceAudioImportResult.Failed ->
                    updateVoiceSession {
                        it.copy(
                            isLoadingInput = false,
                            inputPcm = shortArrayOf(),
                            inputDisplayName = "",
                            processedPcm = shortArrayOf(),
                            debugMainVoicePcm = shortArrayOf(),
                            debugSubvoicePcm = shortArrayOf(),
                            debugSignalOverlayPcm = shortArrayOf(),
                            isPreviewPlaying = false,
                            lastErrorCode = AudioIoWavCodes.STATUS_INVALID_HEADER,
                        )
                    }
            }
        }
    }

    fun onProcessRecording() {
        val session = uiState.value.voiceSession
        if (!session.canProcess) {
            return
        }
        stopGlobalPlayback()
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                isProcessing = true,
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        val inputPcm = session.inputPcm.copyOf()
        val sampleRateHz = session.sampleRateHz
        val preset = session.selectedPreset
        val subvoiceStyle = session.selectedSubvoiceStyle
        processRecordedBatch(
            pcm = inputPcm,
            sampleRateHz = sampleRateHz,
            preset = preset,
            subvoiceStyle = subvoiceStyle,
        )
    }

    private fun processRecordedBatch(
        pcm: ShortArray,
        sampleRateHz: Int,
        preset: VoiceFxPresetOption,
        subvoiceStyle: VoiceFxSubvoiceStyleOption,
    ) {
        scope.launch(workerDispatcher) {
            when (
                val result =
                    voiceFxGateway.applyVoiceFx(
                        preset = preset,
                        subvoiceStyle = subvoiceStyle,
                        pcm = pcm,
                        sampleRateHz = sampleRateHz,
                    )
            ) {
                is VoiceFxProcessResult.Success -> {
                    VoiceFxRecordDiag.log(
                        "batchProcess ok preset=${preset.id} style=${subvoiceStyle.id} sr=$sampleRateHz " +
                            "input=${pcm.size} final=${result.finalMix.size} main=${result.mainVoice.size} " +
                            "sub=${result.subvoice.size} signal=${result.signalOverlay.size} " +
                            "ratio=${formatRecordRatio(result.finalMix.size, pcm.size)}",
                    )
                    updateVoiceSession {
                        it.copy(
                            isProcessing = false,
                            processedPcm = result.finalMix,
                            debugMainVoicePcm = result.mainVoice,
                            debugSubvoicePcm = result.subvoice,
                            debugSignalOverlayPcm = result.signalOverlay,
                            isPreviewPlaying = false,
                            previewTrack = null,
                            previewPositionSamples = 0,
                            lastErrorCode = BagApiCodes.ERROR_OK,
                        )
                    }
                }

                is VoiceFxProcessResult.Failed -> {
                    VoiceFxRecordDiag.log(
                        "batchProcess failed preset=${preset.id} style=${subvoiceStyle.id} sr=$sampleRateHz " +
                            "input=${pcm.size} error=${result.errorCode}",
                    )
                    updateVoiceSession {
                        it.copy(
                            isProcessing = false,
                            processedPcm = shortArrayOf(),
                            debugMainVoicePcm = shortArrayOf(),
                            debugSubvoicePcm = shortArrayOf(),
                            debugSignalOverlayPcm = shortArrayOf(),
                            isPreviewPlaying = false,
                            previewTrack = null,
                            previewPositionSamples = 0,
                            lastErrorCode = result.errorCode,
                        )
                    }
                }
            }
        }
    }

    fun onTogglePreview() {
        onTogglePreviewTrack(VoicePreviewTrackOption.Output)
    }

    fun onTogglePreviewTrack(track: VoicePreviewTrackOption) {
        val session = uiState.value.voiceSession
        val pcm =
            when (track) {
                VoicePreviewTrackOption.Input -> session.inputPcm
                VoicePreviewTrackOption.Output -> session.processedPcm
            }
        if (pcm.isEmpty() && !(session.isPreviewPlaying && session.previewTrack == track)) {
            return
        }
        if (session.isPreviewPlaying && session.previewTrack == track) {
            stopPreviewInternal()
            return
        }
        if (session.isRecording || session.isLoadingInput || session.isProcessing || session.isLiveActive) {
            return
        }
        stopGlobalPlayback()
        stopPreviewInternal()
        val startSampleIndex =
            if (session.previewTrack == track) {
                session.previewPositionSamples
            } else {
                0
            }
        voicePreviewPlayer.play(
            scope = scope,
            pcm = pcm.copyOf(),
            sampleRateHz = session.sampleRateHz,
            startSampleIndex = startSampleIndex,
            onPlaybackStarted = {
                updateVoiceSession { current ->
                    current.copy(
                        isPreviewPlaying = true,
                        previewTrack = track,
                        previewPositionSamples = startSampleIndex,
                    )
                }
            },
            onProgressChanged = { playedSamples, _ ->
                updateVoiceSession { current ->
                    if (current.previewTrack == track) {
                        current.copy(previewPositionSamples = playedSamples)
                    } else {
                        current
                    }
                }
            },
            onPlaybackFinished = {
                updateVoiceSession { current ->
                    if (current.previewTrack == track) {
                        current.copy(
                            isPreviewPlaying = false,
                            previewPositionSamples = 0,
                        )
                    } else {
                        current.copy(isPreviewPlaying = false)
                    }
                }
            },
        )
    }

    fun onPreviewTrackSeek(
        track: VoicePreviewTrackOption,
        targetSamples: Int,
    ) {
        val session = uiState.value.voiceSession
        val totalSamples =
            when (track) {
                VoicePreviewTrackOption.Input -> session.inputPcm.size
                VoicePreviewTrackOption.Output -> session.processedPcm.size
            }
        val clampedTarget = targetSamples.coerceIn(0, totalSamples)
        if (session.isPreviewPlaying && session.previewTrack == track) {
            voicePreviewPlayer.seekTo(clampedTarget)?.let { resolved ->
                updateVoiceSession { it.copy(previewPositionSamples = resolved) }
                return
            }
        }
        updateVoiceSession {
            it.copy(
                previewTrack = track,
                previewPositionSamples = clampedTarget,
            )
        }
    }

    fun onStartLive() {
        val session = uiState.value.voiceSession
        if (!session.canStartLive) {
            return
        }
        stopGlobalPlayback()
        stopPreviewInternal()
        updateVoiceSession {
            it.copy(
                isRecording = false,
                isLoadingInput = false,
                isProcessing = false,
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                isLiveActive = true,
                liveInputRouteLabel = "",
                liveOutputRouteLabel = "",
                liveSpeakerOutRequested = true,
                liveSpeakerOutActive = false,
                lastErrorCode = BagApiCodes.ERROR_OK,
            )
        }
        val started =
            voiceLiveGateway.start(
                config =
                    VoiceLiveConfig(
                        sampleRateHz = LIVE_SAMPLE_RATE_HZ,
                        preset = session.selectedPreset,
                        subvoiceStyle = session.selectedSubvoiceStyle,
                    ),
                onRouteChanged = { snapshot ->
                    updateVoiceSession { current ->
                        current.copy(
                            liveInputRouteLabel = snapshot.inputRouteLabel,
                            liveOutputRouteLabel = snapshot.outputRouteLabel,
                            liveSpeakerOutRequested = snapshot.speakerOutputRequested,
                            liveSpeakerOutActive = snapshot.speakerOutputActive,
                        )
                    }
                },
                onStopped = { errorCode ->
                    updateVoiceSession { current ->
                        current.copy(
                            isLiveActive = false,
                            lastErrorCode =
                                if (errorCode == BagApiCodes.ERROR_OK) {
                                    current.lastErrorCode
                                } else {
                                    errorCode
                                },
                        )
                    }
                },
            )
        if (!started) {
            updateVoiceSession { it.copy(isLiveActive = false, lastErrorCode = BagApiCodes.ERROR_INTERNAL) }
            return
        }
    }

    fun onStopLive() {
        stopLiveInternal()
    }

    fun onRequestExportProcessedAudioToDocument() {
        val session = uiState.value.voiceSession
        if (!session.canExportResult) {
            logExportFailure(
                "document export request ignored: canExportResult=false " +
                    "hasProcessedAudio=${session.hasProcessedAudio} processedSamples=${session.processedPcm.size} " +
                    "sampleRateHz=${session.sampleRateHz} preset=${session.selectedPreset.id} " +
                    "inputSource=${session.selectedInputSource.id}",
            )
            return
        }
        uiState.update { state ->
            state.copy(
                pendingDocumentExportRequest =
                    PendingAudioDocumentExportRequest(
                        id = System.nanoTime(),
                        suggestedFileName = buildSuggestedExportFileName(session),
                        source = AudioDocumentExportSource.Voice,
                    ),
            )
        }
    }

    fun onShareProcessedAudio() {
        val session = uiState.value.voiceSession
        val repository = savedAudioRepository
        if (!session.canExportResult || repository == null) {
            logExportFailure(
                "share ignored: canExportResult=${session.canExportResult} repositoryPresent=${repository != null} " +
                    "hasProcessedAudio=${session.hasProcessedAudio} processedSamples=${session.processedPcm.size} " +
                    "sampleRateHz=${session.sampleRateHz} preset=${session.selectedPreset.id} " +
                    "inputSource=${session.selectedInputSource.id}",
            )
            return
        }
        val pcm = session.processedPcm.copyOf()
        val sampleRateHz = session.sampleRateHz
        val displayName = buildSuggestedExportFileName(session)
        logExportDebug(
            "share start displayName=$displayName processedSamples=${pcm.size} sampleRateHz=$sampleRateHz " +
                "preset=${session.selectedPreset.id} inputSource=${session.selectedInputSource.id}",
        )
        scope.launch(workerDispatcher) {
            val shared =
                runCatching {
                    repository.shareRawPcmAudio(
                        displayName = displayName,
                        pcm = pcm,
                        sampleRateHz = sampleRateHz,
                    )
                }.getOrElse { throwable ->
                    logExportFailure("share threw displayName=$displayName", throwable)
                    false
                }
            if (!shared) {
                logExportFailure(
                    "share returned false displayName=$displayName processedSamples=${pcm.size} " +
                        "sampleRateHz=$sampleRateHz",
                )
                emitSnackbar(UiText.Resource(R.string.snackbar_audio_export_to_file_failed))
            } else {
                logExportDebug("share success displayName=$displayName")
            }
        }
    }

    fun onDocumentExportPicked(uriString: String?) {
        val request = uiState.value.pendingDocumentExportRequest ?: return
        uiState.update { it.copy(pendingDocumentExportRequest = null) }
        if (request.source != AudioDocumentExportSource.Voice || uriString == null) {
            logExportDebug("document export picker ignored source=${request.source} uriPresent=${uriString != null}")
            return
        }
        val session = uiState.value.voiceSession
        if (!session.hasProcessedAudio) {
            logExportFailure(
                "document export failed before gateway: hasProcessedAudio=false " +
                    "processedSamples=${session.processedPcm.size} sampleRateHz=${session.sampleRateHz}",
            )
            emitSnackbar(UiText.Resource(R.string.snackbar_audio_export_to_file_failed))
            return
        }
        val pcm = session.processedPcm.copyOf()
        val sampleRateHz = session.sampleRateHz
        logExportDebug(
            "document export start uri=$uriString processedSamples=${pcm.size} sampleRateHz=$sampleRateHz " +
                "suggestedFileName=${request.suggestedFileName}",
        )
        scope.launch(workerDispatcher) {
            val exported = voiceAudioFileGateway.exportVoiceAudioToDocument(pcm, sampleRateHz, uriString)
            if (!exported) {
                logExportFailure(
                    "document export gateway returned false uri=$uriString processedSamples=${pcm.size} " +
                        "sampleRateHz=$sampleRateHz suggestedFileName=${request.suggestedFileName}",
                )
            } else {
                logExportDebug("document export success uri=$uriString suggestedFileName=${request.suggestedFileName}")
            }
            emitSnackbar(
                UiText.Resource(
                    if (exported) {
                        R.string.snackbar_audio_exported_to_file
                    } else {
                        R.string.snackbar_audio_export_to_file_failed
                    },
                ),
            )
        }
    }

    fun onClear() {
        stopPreviewInternal()
        stopLiveInternal()
        if (uiState.value.voiceSession.isRecording) {
            voiceRecordingGateway.stopRecording()
        }
        resetRecordProcessingSession()
        val previous = uiState.value.voiceSession
        uiState.update { state ->
            state.copy(
                voiceSession =
                    VoiceSessionState(
                        hasRecordPermission = previous.hasRecordPermission,
                        selectedWorkflowMode = previous.selectedWorkflowMode,
                        selectedTrackMode = previous.selectedTrackMode,
                        selectedInputSource = previous.selectedInputSource,
                        selectedRecordProcessingMode = previous.selectedRecordProcessingMode,
                        sampleRateHz = previous.sampleRateHz,
                        selectedPreset = previous.selectedPreset,
                        selectedSubvoiceStyle = previous.selectedSubvoiceStyle,
                        previewTrack = null,
                        previewPositionSamples = 0,
                    ),
            )
        }
    }

    fun onLeaveVoiceTab() {
        stopPreviewInternal()
        stopLiveInternal()
        val session = uiState.value.voiceSession
        if (!session.isRecording) {
            return
        }
        val recorded = voiceRecordingGateway.stopRecording()
        resetRecordProcessingSession()
        updateVoiceSession {
            it.copy(
                isRecording = false,
                isLoadingInput = false,
                inputPcm = recorded,
                inputDisplayName = "",
                processedPcm = shortArrayOf(),
                debugMainVoicePcm = shortArrayOf(),
                debugSubvoicePcm = shortArrayOf(),
                debugSignalOverlayPcm = shortArrayOf(),
                isPreviewPlaying = false,
                previewTrack = null,
                previewPositionSamples = 0,
                lastErrorCode = if (recorded.isNotEmpty()) BagApiCodes.ERROR_OK else BagApiCodes.ERROR_INTERNAL,
            )
        }
    }

    fun stopPreviewForExternalPlayback() {
        stopPreviewInternal()
        stopLiveInternal()
    }

    fun release() {
        stopPreviewInternal()
        stopLiveInternal()
        voiceRecordingGateway.release()
        voiceLiveGateway.release()
        resetRecordProcessingSession()
    }

    private fun stopPreviewInternal() {
        voicePreviewPlayer.stop()
        updateVoiceSession {
            if (it.isPreviewPlaying) {
                it.copy(isPreviewPlaying = false)
            } else {
                it
            }
        }
    }

    private fun updateVoiceSession(transform: (VoiceSessionState) -> VoiceSessionState) {
        uiState.update { state ->
            state.copy(voiceSession = transform(state.voiceSession))
        }
    }

    private fun VoiceSessionState.isVoiceTransitionBlocked(): Boolean = isRecording || isLoadingInput || isProcessing || isLiveActive

    private fun stopLiveInternal() {
        val wasActive = uiState.value.voiceSession.isLiveActive
        voiceLiveGateway.stop()
        if (wasActive) {
            updateVoiceSession { it.copy(isLiveActive = false) }
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

    private fun buildSuggestedExportFileName(session: VoiceSessionState): String {
        val sourceStem =
            sanitizeFileStem(
                when {
                    session.inputDisplayName.isNotBlank() -> session.inputDisplayName.replace(TrailingAudioExtension, "")
                    session.selectedInputSource == VoiceInputSourceOption.Record -> "recorded_voice"
                    else -> "voice_input"
                },
            )
        return "${session.selectedPreset.id}_$sourceStem.wav"
    }

    private fun sanitizeFileStem(raw: String): String =
        raw
            .replace(IllegalFileNameCharacters, "_")
            .replace(WhitespaceRegex, "_")
            .trim('_', '.', ' ')
            .take(MAX_FILE_STEM_LENGTH)
            .ifBlank { "voice_result" }

    private fun finishRecordedProcessing(): VoiceFxStreamCollectedResult =
        recordProcessingCollector
            ?.finish()
            ?.also { recordProcessingCollector = null }
            ?: VoiceFxStreamCollectedResult(
                finalMix = shortArrayOf(),
                errorCode = BagApiCodes.ERROR_INTERNAL,
            )

    private fun resetRecordProcessingSession() {
        recordProcessingCollector?.release()
        recordProcessingCollector = null
    }

    private companion object {
        const val RECORDING_SAMPLE_RATE_HZ = 44100
        const val LIVE_SAMPLE_RATE_HZ = 44100
        const val EXPORT_SNACKBAR_DURATION_MILLIS = 1400L
        const val MAX_FILE_STEM_LENGTH = 32
        const val VOICE_EXPORT_TAG = "VoiceExportDiag"
        val WhitespaceRegex = Regex("\\s+")
        val IllegalFileNameCharacters = Regex("[\\\\/:*?\"<>|]+")
        val TrailingAudioExtension = Regex("\\.[A-Za-z0-9]{1,5}$")

        fun formatRecordRatio(
            outputSamples: Int,
            inputSamples: Int,
        ): String =
            if (inputSamples > 0) {
                "%.3f".format(outputSamples.toDouble() / inputSamples.toDouble())
            } else {
                "n/a"
            }

        fun logExportDebug(message: String) {
            try {
                Log.d(VOICE_EXPORT_TAG, message)
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
            }
        }

        fun logExportFailure(
            message: String,
            throwable: Throwable? = null,
        ) {
            try {
                if (throwable == null) {
                    Log.e(VOICE_EXPORT_TAG, message)
                } else {
                    Log.e(VOICE_EXPORT_TAG, message, throwable)
                }
            } catch (_: RuntimeException) {
                // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
            }
        }
    }
}
