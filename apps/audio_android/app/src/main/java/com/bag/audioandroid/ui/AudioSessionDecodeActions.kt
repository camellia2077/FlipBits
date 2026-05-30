package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.data.readPcmSegmentsFromFile
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodeProgressUpdate
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackDetailsSource
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.LinkedHashSet

internal class AudioSessionDecodeActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val audioCodecGateway: AudioCodecGateway,
    sessionStateStore: AudioSessionStateStore,
    uiTextMapper: BagUiTextMapper,
    private val sampleRateHz: Int,
    private val frameSamples: Int,
    private val workerDispatcher: CoroutineDispatcher,
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
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
            safeLogE(
                SAVED_DECODE_PROGRESS_LOG_TAG,
                "onDecode skipped busy source=${current.currentPlaybackSource}",
            )
            return
        }
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "onDecode source=${current.currentPlaybackSource} selectedSaved=${current.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        when (val source = current.currentPlaybackSource) {
            is AudioPlaybackSource.Generated -> onDecodeGenerated(current, source.mode)
            is AudioPlaybackSource.Saved -> onDecodeSaved(current, source.itemId)
        }
    }

    fun ensureCurrentPlaybackDecodedForLyrics() {
        val current = uiState.value
        if (current.currentSession.isCodecBusy) {
            safeLogE(
                SAVED_DECODE_PROGRESS_LOG_TAG,
                "ensureSavedDecode skipped busy source=${current.currentPlaybackSource}",
            )
            return
        }
        val source =
            current.currentPlaybackSource as? AudioPlaybackSource.Saved ?: run {
                safeLogE(
                    SAVED_DECODE_PROGRESS_LOG_TAG,
                    "ensureSavedDecode skipped nonSaved source=${current.currentPlaybackSource}",
                )
                return
            }
        val selectedSavedAudio =
            current.selectedSavedAudio
                ?.takeIf { it.item.itemId == source.itemId }
                ?: run {
                    safeLogE(
                        SAVED_DECODE_PROGRESS_LOG_TAG,
                        "ensureSavedDecode skipped noSelection itemId=${source.itemId} selected=${current.selectedSavedAudio?.item?.itemId.orEmpty()}",
                    )
                    return
                }
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "ensureSavedDecode itemId=${source.itemId} loading=${selectedSavedAudio.isLoadingContent} " +
                "decoding=${selectedSavedAudio.isDecodingContent} needsDecoded=${selectedSavedAudio.needsDecodedContent} " +
                "textStatus=${selectedSavedAudio.decodedPayload.textDecodeStatusCode} " +
                "followAvailable=${selectedSavedAudio.followData.textFollowAvailable}",
        )
        if (selectedSavedAudio.isLoadingContent) {
            safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "ensureSavedDecode skipped loading itemId=${source.itemId}")
            return
        }
        if (selectedSavedAudio.isDecodingContent) {
            safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "ensureSavedDecode skipped alreadyDecoding itemId=${source.itemId}")
            return
        }
        val alreadyDecodedForLyrics =
            selectedSavedAudio.decodedPayload.textDecodeStatusCode !=
                BagDecodeContentCodes.STATUS_UNAVAILABLE ||
                selectedSavedAudio.followData.textFollowAvailable
        if (alreadyDecodedForLyrics) {
            safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "ensureSavedDecode skipped alreadyDecoded itemId=${source.itemId}")
            return
        }
        safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "ensureSavedDecode start itemId=${source.itemId}")
        onDecodeSaved(current, source.itemId)
    }

    private fun onDecodeGenerated(
        current: AudioAppUiState,
        mode: TransportModeOption,
    ) {
        val session = current.sessions.getValue(mode)
        if (session.generatedPcm.isEmpty() && session.generatedPcmFilePath.isNullOrBlank()) {
            stateReducer.applyNoGeneratedAudio(mode)
            return
        }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeGenerated:request mode=${mode.wireName} " +
                "inMemorySamples=${session.generatedPcm.size} " +
                "waveformSamples=${session.generatedWaveformPcm.size} " +
                "fileBacked=${!session.generatedPcmFilePath.isNullOrBlank()} " +
                "metadataSamples=${session.generatedAudioMetadata?.pcmSampleCount ?: 0}",
        )
        val request =
            requestFactory.buildGenerated(
                current = current,
                mode = mode,
                generatedPcm = session.generatedPcm,
                generatedPcmFilePath = session.generatedPcmFilePath,
                metadata = session.generatedAudioMetadata,
                fallbackFlashStyle = current.selectedFlashVoicingStyle,
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
                ?: run {
                    safeLogE(
                        SAVED_DECODE_PROGRESS_LOG_TAG,
                        "onDecodeSaved skipped noSelection itemId=$itemId selected=${current.selectedSavedAudio?.item?.itemId.orEmpty()}",
                    )
                    return
                }
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "onDecodeSaved itemId=$itemId loading=${selectedSavedAudio.isLoadingContent} " +
                "decoding=${selectedSavedAudio.isDecodingContent} needsDecoded=${selectedSavedAudio.needsDecodedContent} " +
                "mode=${selectedSavedAudio.item.modeWireName} fileBacked=${!selectedSavedAudio.pcmFilePath.isNullOrBlank()} " +
                "inMemorySamples=${selectedSavedAudio.pcm.size} metadataSamples=${selectedSavedAudio.metadata?.pcmSampleCount ?: 0}",
        )
        if (selectedSavedAudio.isLoadingContent || selectedSavedAudio.isDecodingContent) {
            safeLogE(
                SAVED_DECODE_PROGRESS_LOG_TAG,
                "onDecodeSaved skipped loadingOrDecoding itemId=$itemId loading=${selectedSavedAudio.isLoadingContent} " +
                    "decoding=${selectedSavedAudio.isDecodingContent}",
            )
            return
        }
        val mode = TransportModeOption.fromWireName(selectedSavedAudio.item.modeWireName)
        if (mode == null) {
            safeLogE(
                SAVED_DECODE_PROGRESS_LOG_TAG,
                "onDecodeSaved failed unknownMode itemId=$itemId mode=${selectedSavedAudio.item.modeWireName}",
            )
            stateReducer.applySavedLoadFailure()
            return
        }
        safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "onDecodeSaved launch itemId=$itemId mode=${mode.wireName}")
        launchSavedDecode(current, mode, itemId, selectedSavedAudio)
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
        current: AudioAppUiState,
        mode: TransportModeOption,
        itemId: String,
        selectedSavedAudio: SavedAudioPlaybackSelection,
    ) {
        stateReducer.markBusy(mode)
        stateReducer.markSavedDecodeStarted(itemId)
        scope.launch {
            val progressReporter =
                SavedDecodeProgressReporter { update ->
                    stateReducer.applySavedDecodeProgress(itemId, update)
                }
            progressReporter.reportPreparing()
            val request =
                kotlinx.coroutines.withContext(workerDispatcher) {
                    val startedNanos = System.nanoTime()
                    val builtRequest =
                        requestFactory.buildSaved(
                            current = current,
                            mode = mode,
                            savedAudio = selectedSavedAudio,
                            fallbackFlashStyle = current.selectedFlashVoicingStyle,
                            onPcmLoadProgress = progressReporter::reportAudioDataLoading,
                        )
                    safeLogE(
                        SAVED_DECODE_PROGRESS_LOG_TAG,
                        "buildSavedDecodeRequest elapsedMs=${elapsedMsSince(startedNanos)} " +
                            "itemId=$itemId fileBacked=${!selectedSavedAudio.pcmFilePath.isNullOrBlank()} " +
                            "inMemorySamples=${selectedSavedAudio.pcm.size} segmentedCount=${builtRequest.segmentCount}",
                    )
                    builtRequest
                }
            val result =
                decodeRunner.execute(request) { update ->
                    progressReporter.reportNativeDecode(update)
                }
            when (result) {
                is DecodeResult.ValidationFailure ->
                    stateReducer.reduceSavedValidationFailure(itemId, result.validationIssue)

                is DecodeResult.Success -> {
                    progressReporter.reportPlaybackDataBuilding(0f)
                    val flashSignalInfo =
                        kotlinx.coroutines.withContext(workerDispatcher) {
                            val startedNanos = System.nanoTime()
                            val info = describeSavedFlashSignal(selectedSavedAudio, result.decoded.decodedPayload)
                            savedAudioDecodeCacheGateway.write(
                                item = selectedSavedAudio.item,
                                metadata = selectedSavedAudio.metadata,
                                decodedPayload = result.decoded.decodedPayload,
                                followData = result.decoded.followData,
                                flashSignalInfo = info,
                            )
                            safeLogE(
                                SAVED_DECODE_PROGRESS_LOG_TAG,
                                "buildSavedPlaybackData elapsedMs=${elapsedMsSince(startedNanos)} " +
                                    "itemId=$itemId textChars=${result.decoded.decodedPayload.text.length} " +
                                    "followAvailable=${result.decoded.followData.followAvailable}",
                            )
                            info
                        }
                    progressReporter.reportPlaybackDataBuilding(1f)
                    stateReducer.reduceSavedSuccess(
                        itemId = itemId,
                        mode = mode,
                        decoded = result.decoded,
                        flashSignalInfo = flashSignalInfo,
                    )
                    uiState.update { state ->
                        state.copy(decodedSavedAudioItemIds = state.decodedSavedAudioItemIds + itemId)
                    }
                }
            }
        }
    }

    private fun describeSavedFlashSignal(
        savedAudio: SavedAudioPlaybackSelection,
        decodedPayload: DecodedPayloadViewData,
    ): FlashSignalInfo {
        val metadata = savedAudio.metadata ?: return FlashSignalInfo.Empty
        if (metadata.mode != TransportModeOption.Flash) {
            return FlashSignalInfo.Empty
        }
        val resolvedText = decodedPayload.text.takeIf { decodedPayload.hasTextResult && it.isNotBlank() } ?: return FlashSignalInfo.Empty
        val style = savedAudio.item.flashVoicingStyle ?: metadata.flashVoicingStyle ?: return FlashSignalInfo.Empty
        return audioCodecGateway.describeFlashSignal(
            resolvedText,
            savedAudio.sampleRateHz,
            metadata.frameSamples,
            style.signalProfileValue,
            style.voicingFlavorValue,
        )
    }
}

private const val LONG_AUDIO_LOG_TAG = "FlipBitsLongAudio"
private const val SAVED_DECODE_PROGRESS_LOG_TAG = "SavedAudioDecodeProgress"

private fun elapsedMsSince(startedNanos: Long): Long = (System.nanoTime() - startedNanos).coerceAtLeast(0L) / 1_000_000L

private fun safeLogE(
    tag: String,
    message: String,
) {
    try {
        Log.e(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
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
        generatedPcmFilePath: String?,
        metadata: com.bag.audioandroid.domain.GeneratedAudioMetadata?,
        fallbackFlashStyle: FlashVoicingStyleOption,
    ): DecodeRequest {
        val segmentedPcm =
            when {
                generatedPcm.isNotEmpty() ->
                    metadata?.segmentSampleCounts?.let { splitPcmIntoSegments(generatedPcm, it) }
                !generatedPcmFilePath.isNullOrBlank() ->
                    metadata?.let {
                        readPcmSegmentsFromFile(
                            generatedPcmFilePath,
                            it.segmentSampleCounts.takeIf { counts -> counts.isNotEmpty() } ?: listOf(it.pcmSampleCount),
                        )
                    }
                else -> null
            }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeRequest:generated mode=${mode.wireName} inMemorySamples=${generatedPcm.size} fileBacked=${!generatedPcmFilePath
                .isNullOrBlank()} metadataSamples=${metadata?.pcmSampleCount ?: 0} segmentedCount=${segmentedPcm?.size ?: 0}",
        )
        return DecodeRequest(
            mode = mode,
            generatedPcm = generatedPcm,
            generatedPcmFilePath = generatedPcmFilePath,
            sampleRateHz = sampleRateHz,
            frameSamples = metadata?.frameSamples ?: frameSamples,
            segmentedPcm = segmentedPcm,
            flashPresets =
                flashPresetCandidates(
                    mode = mode,
                    preferred = current.sessions.getValue(mode).generatedFlashVoicingStyle,
                    fallback = fallbackFlashStyle,
                ),
            expectedPayloadByteCount = metadata?.payloadByteCount,
        )
    }

    fun buildSaved(
        current: AudioAppUiState,
        mode: TransportModeOption,
        savedAudio: SavedAudioPlaybackSelection,
        fallbackFlashStyle: FlashVoicingStyleOption,
        onPcmLoadProgress: (completedSamples: Int, totalSamples: Int) -> Unit = { _, _ -> },
    ): DecodeRequest {
        val segmentSampleCounts =
            savedAudio.metadata?.segmentSampleCounts?.takeIf { counts -> counts.isNotEmpty() }
                ?: savedAudio.metadata?.pcmSampleCount?.let(::listOf)
        val segmentedPcm =
            when {
                savedAudio.pcm.isNotEmpty() ->
                    savedAudio.metadata?.segmentSampleCounts?.takeIf { it.isNotEmpty() }?.let {
                        splitPcmIntoSegments(savedAudio.pcm, it)
                    }
                else -> null
            }
        val fileBackedSegmentSampleCounts =
            if (segmentedPcm == null && !savedAudio.pcmFilePath.isNullOrBlank()) {
                segmentSampleCounts
            } else {
                null
            }
        safeLogE(
            LONG_AUDIO_LOG_TAG,
            "decodeRequest:saved mode=${mode.wireName} " +
                "itemId=${savedAudio.item.itemId} " +
                "inMemorySamples=${savedAudio.pcm.size} " +
                "fileBacked=${!savedAudio.pcmFilePath.isNullOrBlank()} " +
                "metadataSamples=${savedAudio.metadata?.pcmSampleCount ?: 0} " +
                "segmentedCount=${segmentedPcm?.size ?: fileBackedSegmentSampleCounts?.size ?: 0}",
        )
        return DecodeRequest(
            mode = mode,
            generatedPcm = savedAudio.pcm,
            generatedPcmFilePath = savedAudio.pcmFilePath,
            sampleRateHz = savedAudio.sampleRateHz,
            frameSamples = savedAudio.metadata?.frameSamples ?: frameSamples,
            segmentedPcm = segmentedPcm,
            fileBackedSegmentSampleCounts = fileBackedSegmentSampleCounts,
            onPcmLoadProgress = onPcmLoadProgress,
            flashPresets =
                flashPresetCandidates(
                    mode = mode,
                    preferred = savedAudio.item.flashVoicingStyle,
                    fallback = fallbackFlashStyle,
                ),
            expectedPayloadByteCount = savedAudio.metadata?.payloadByteCount,
        )
    }

    private fun flashPresetCandidates(
        mode: TransportModeOption,
        preferred: FlashVoicingStyleOption?,
        fallback: FlashVoicingStyleOption,
    ): List<FlashVoicingStyleOption> {
        if (mode != TransportModeOption.Flash) {
            return listOf(FlashVoicingStyleOption.Standard)
        }
        val ordered = LinkedHashSet<FlashVoicingStyleOption>()
        preferred?.let(ordered::add)
        ordered += fallback
        ordered += FlashVoicingStyleOption.entries
        return ordered.toList()
    }
}

private class DecodeRunner(
    private val audioCodecGateway: AudioCodecGateway,
    private val workerDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(request: DecodeRequest): DecodeResult = execute(request, onProgress = {})

    suspend fun execute(
        request: DecodeRequest,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodeResult =
        kotlinx.coroutines.withContext(workerDispatcher) {
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:start mode=${request.mode.wireName} " +
                    "inMemorySamples=${request.generatedPcm.size} " +
                    "fileBacked=${!request.generatedPcmFilePath.isNullOrBlank()} " +
                    "segmentedCount=${request.segmentCount} " +
                    "sampleRate=${request.sampleRateHz} " +
                    "frameSamples=${request.frameSamples}",
            )
            val validationIssue =
                audioCodecGateway.validateDecodeConfig(
                    request.sampleRateHz,
                    request.frameSamples,
                    request.mode.nativeValue,
                    request.flashPresets.first().signalProfileValue,
                    request.flashPresets.first().voicingFlavorValue,
                )
            if (validationIssue != BagApiCodes.VALIDATION_OK) {
                return@withContext DecodeResult.ValidationFailure(validationIssue)
            }

            val decoded = decodeWithFallback(request, onProgress)
            val decodedText = decoded.decodedPayload.text
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:done mode=${request.mode.wireName} decodedStatus=${decoded.decodedPayload.textDecodeStatusCode} " +
                    "followAvailable=${decoded.followData.followAvailable} " +
                    "textFollowAvailable=${decoded.followData.textFollowAvailable} " +
                    "textTokens=${decoded.followData.textTokens.size} " +
                    "textTimeline=${decoded.followData.textTokenTimeline.size} " +
                    "binaryGroups=${decoded.followData.binaryGroupTimeline.size} " +
                    "textChars=${decodedText.length} " +
                    "textWhitespace=${decodedText.count(Char::isWhitespace)} rawBytesHex=${decoded.decodedPayload.rawBytesHex}",
            )

            DecodeResult.Success(decoded)
        }

    private suspend fun decodeWithFallback(
        request: DecodeRequest,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodedAudioPayloadResult {
        if (request.mode != TransportModeOption.Flash) {
            return decodeWithPreset(request, request.flashPresets.first(), onProgress)
        }
        val attempts = mutableListOf<DecodeAttempt>()
        request.flashPresets.forEach { preset ->
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:attempt mode=${request.mode.wireName} preset=${preset.id} expectedPayloadBytes=${request.expectedPayloadByteCount ?: -1}",
            )
            val attempt =
                DecodeAttempt(
                    preset = preset,
                    result = decodeWithPreset(request, preset, onProgress),
                )
            attempts += attempt
            if (isStrongDecodeMatch(attempt, request.expectedPayloadByteCount)) {
                return attempt.result
            }
        }
        return attempts
            .maxWithOrNull(compareBy<DecodeAttempt> { scoreDecodeAttempt(it, request.expectedPayloadByteCount) })
            ?.result
            ?: decodeWithPreset(request, FlashVoicingStyleOption.Standard, onProgress)
    }

    private suspend fun decodeWithPreset(
        request: DecodeRequest,
        preset: FlashVoicingStyleOption,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodedAudioPayloadResult =
        request.segmentedPcm?.let { segmentedPcm ->
            safeLogE(
                LONG_AUDIO_LOG_TAG,
                "decodeRunner:segmented mode=${request.mode.wireName} segments=${segmentedPcm.size} preset=${preset.id}",
            )
            mergeSegmentedDecodedPayloadResults(
                segmentedPcm.mapIndexed { index, segmentPcm ->
                    audioCodecGateway.decodeGeneratedPcm(
                        segmentPcm,
                        request.sampleRateHz,
                        request.frameSamples,
                        request.mode.nativeValue,
                        preset.signalProfileValue,
                        preset.voicingFlavorValue,
                        onProgress =
                            aggregateSegmentProgressCallback(
                                segmentIndex = index,
                                segmentCount = segmentedPcm.size,
                                onProgress = onProgress,
                            ),
                    )
                },
            )
        }
            ?: request.fileBackedSegmentSampleCounts?.let { segmentSampleCounts ->
                val pcmFilePath = request.generatedPcmFilePath
                if (pcmFilePath.isNullOrBlank()) {
                    return@let null
                }
                safeLogE(
                    LONG_AUDIO_LOG_TAG,
                    "decodeRunner:fileSegmented mode=${request.mode.wireName} segments=${segmentSampleCounts.size} preset=${preset.id}",
                )
                var segmentStartSample = 0L
                request.onPcmLoadProgress(request.totalFileBackedSamples, request.totalFileBackedSamples)
                mergeSegmentedDecodedPayloadResults(
                    segmentSampleCounts.mapIndexed { index, segmentSampleCount ->
                        val decoded =
                            audioCodecGateway.decodePcmFileSegment(
                                pcmFilePath = pcmFilePath,
                                startSample = segmentStartSample,
                                sampleCount = segmentSampleCount,
                                sampleRateHz = request.sampleRateHz,
                                frameSamples = request.frameSamples,
                                mode = request.mode.nativeValue,
                                flashSignalProfile = preset.signalProfileValue,
                                flashVoicingFlavor = preset.voicingFlavorValue,
                            )
                        segmentStartSample += segmentSampleCount.toLong()
                        onProgress(
                            segmentedDecodeProgressUpdate(
                                segmentIndex = index,
                                segmentCount = segmentSampleCounts.size,
                                segmentProgress0To1 = 1f,
                            ),
                        )
                        decoded
                    },
                )
            }
            ?: audioCodecGateway.decodeGeneratedPcm(
                request.generatedPcm,
                request.sampleRateHz,
                request.frameSamples,
                request.mode.nativeValue,
                preset.signalProfileValue,
                preset.voicingFlavorValue,
                onProgress,
            )

    private fun aggregateSegmentProgressCallback(
        segmentIndex: Int,
        segmentCount: Int,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): (DecodeProgressUpdate) -> Unit =
        { update ->
            val segmentProgress = update.snapshot.overallProgress0To1.coerceIn(0f, 1f)
            val aggregateProgress =
                aggregateSegmentedDecodeProgress0To1(
                    segmentIndex = segmentIndex,
                    segmentCount = segmentCount,
                    segmentProgress0To1 = segmentProgress,
                )
            onProgress(
                update.copy(
                    progress0To1 = aggregateProgress,
                    snapshot =
                        update.snapshot.copy(
                            overallProgress0To1 = aggregateProgress,
                            completedWorkUnits =
                                aggregateSegmentedDecodeWorkUnits(
                                    segmentIndex = segmentIndex,
                                    segmentCount = segmentCount,
                                    segmentProgress0To1 = segmentProgress,
                                ),
                            totalWorkUnits = SEGMENTED_DECODE_AGGREGATE_WORK_UNITS,
                        ),
                ),
            )
        }

    private fun scoreDecodeAttempt(
        attempt: DecodeAttempt,
        expectedPayloadByteCount: Int?,
    ): Int {
        val payloadByteCount =
            attempt.result.decodedPayload.rawBytesHex
                .split(' ')
                .filter { it.isNotBlank() }
                .size
        val payloadMatchBonus =
            if (expectedPayloadByteCount != null && payloadByteCount == expectedPayloadByteCount) {
                100
            } else {
                0
            }
        val textBonus =
            if (attempt.result.decodedPayload.hasTextResult) {
                10
            } else {
                0
            }
        val rawBonus =
            if (attempt.result.decodedPayload.rawPayloadAvailable) {
                1
            } else {
                0
            }
        return payloadMatchBonus + textBonus + rawBonus
    }

    private fun isStrongDecodeMatch(
        attempt: DecodeAttempt,
        expectedPayloadByteCount: Int?,
    ): Boolean {
        if (!attempt.result.decodedPayload.hasTextResult) {
            return false
        }
        if (expectedPayloadByteCount == null) {
            return true
        }
        val payloadByteCount =
            attempt.result.decodedPayload.rawBytesHex
                .split(' ')
                .count { it.isNotBlank() }
        return payloadByteCount == expectedPayloadByteCount
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
                encodeOperationSnapshot = null,
                encodeOperationWorkPlan = null,
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
                applySavedSuccess(itemId, result.decoded, FlashSignalInfo.Empty, status)
            }
        }
    }

    fun markSavedDecodeStarted(itemId: String) {
        safeLogE(SAVED_DECODE_PROGRESS_LOG_TAG, "start itemId=$itemId")
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        isDecodingContent = true,
                        decodeOperationSnapshot = null,
                        decodeOperationWorkPlan = null,
                    ),
            )
        }
    }

    fun applySavedDecodeProgress(
        itemId: String,
        update: DecodeProgressUpdate,
    ) {
        val snapshot = update.snapshot
        val workPlan = update.workPlan
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "progress itemId=$itemId state=${snapshot.state.name.lowercase()} " +
                "phase=${snapshot.phase.name.lowercase()} percent=${"%.1f".format(snapshot.overallProgress0To1 * 100f)} " +
                "phasePercent=${"%.1f".format(snapshot.phaseProgress0To1 * 100f)} " +
                "completed=${snapshot.completedWorkUnits} total=${snapshot.totalWorkUnits} " +
                "phaseCompleted=${snapshot.phaseCompletedWorkUnits} phaseTotal=${snapshot.phaseTotalWorkUnits} " +
                "terminal=${snapshot.terminalCode} expectedPcm=${workPlan.pcmSampleCount}",
        )
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        decodeOperationSnapshot = update.snapshot,
                        decodeOperationWorkPlan = update.workPlan,
                    ),
            )
        }
    }

    fun reduceSavedValidationFailure(
        itemId: String,
        validationIssue: Int,
    ) {
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "failed itemId=$itemId validationIssue=$validationIssue",
        )
        uiState.update { state ->
            val selected =
                state.selectedSavedAudio
                    ?.takeIf { it.item.itemId == itemId }
                    ?: return@update state
            state.copy(
                selectedSavedAudio =
                    selected.copy(
                        isDecodingContent = false,
                        decodeOperationSnapshot = null,
                        decodeOperationWorkPlan = null,
                    ),
            )
        }
        applyValidationFailure(validationIssue)
    }

    fun reduceSavedSuccess(
        itemId: String,
        mode: TransportModeOption,
        decoded: DecodedAudioPayloadResult,
        flashSignalInfo: FlashSignalInfo,
    ) {
        val status = decodeStatusText(mode, decoded.decodedPayload)
        safeLogE(
            SAVED_DECODE_PROGRESS_LOG_TAG,
            "done itemId=$itemId mode=${mode.wireName} decodedStatus=${decoded.decodedPayload.textDecodeStatusCode} " +
                "textChars=${decoded.decodedPayload.text.length} followAvailable=${decoded.followData.followAvailable}",
        )
        applySavedSuccess(itemId, decoded, flashSignalInfo, status)
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
                encodeOperationSnapshot = null,
                encodeOperationWorkPlan = null,
                isEncodeCancelling = false,
            )
        }
    }

    private fun applySavedSuccess(
        itemId: String,
        decoded: DecodedAudioPayloadResult,
        flashSignalInfo: FlashSignalInfo,
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
                        playbackDetailsSource = PlaybackDetailsSource.FreshDecode,
                        flashSignalInfo = flashSignalInfo,
                        needsDecodedContent = false,
                        isDecodingContent = false,
                        decodeOperationSnapshot = null,
                        decodeOperationWorkPlan = null,
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
                encodeOperationSnapshot = null,
                encodeOperationWorkPlan = null,
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
    val generatedPcmFilePath: String? = null,
    val sampleRateHz: Int,
    val frameSamples: Int,
    val flashPresets: List<FlashVoicingStyleOption>,
    val segmentedPcm: List<ShortArray>? = null,
    val fileBackedSegmentSampleCounts: List<Int>? = null,
    val onPcmLoadProgress: (completedSamples: Int, totalSamples: Int) -> Unit = { _, _ -> },
    val expectedPayloadByteCount: Int? = null,
) {
    val segmentCount: Int
        get() = segmentedPcm?.size ?: fileBackedSegmentSampleCounts?.size ?: 0

    val totalFileBackedSamples: Int
        get() = fileBackedSegmentSampleCounts?.sum() ?: 0
}

private data class DecodeAttempt(
    val preset: FlashVoicingStyleOption,
    val result: DecodedAudioPayloadResult,
)

private sealed interface DecodeResult {
    data class ValidationFailure(
        val validationIssue: Int,
    ) : DecodeResult

    data class Success(
        val decoded: DecodedAudioPayloadResult,
    ) : DecodeResult
}
