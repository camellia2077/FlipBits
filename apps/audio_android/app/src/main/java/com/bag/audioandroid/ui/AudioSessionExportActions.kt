package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.SnackbarMessage
import com.bag.audioandroid.util.safeDebugLog
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
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val audioCodecGateway: AudioCodecGateway? = null,
    private val sampleRateHz: Int,
    private val frameSamples: Int = 2205,
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
            val current = uiState.value
            val session = current.currentSession
            val metadata = session.generatedAudioMetadata
            if ((session.generatedPcm.isEmpty() && session.generatedPcmFilePath == null) || metadata == null) {
                sessionStateStore
                    .updateCurrentSession {
                        it.copy(statusText = UiText.Resource(R.string.status_audio_save_failed))
                    }.also {
                        emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
                    }
                return@launch
            }
            if (
                !savedAudioRepository.shareGeneratedAudio(
                    inputText = session.inputText,
                    pcm = session.generatedPcm,
                    pcmFilePath = session.generatedPcmFilePath,
                    sampleRateHz = sampleRateHz,
                    metadata = metadata,
                )
            ) {
                emitSnackbar(UiText.Resource(R.string.snackbar_audio_save_failed))
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

    private suspend fun handleGeneratedAudioExportSuccess(
        result: AudioExportResult.Success,
        snackbarMessage: UiText,
    ) {
        val current = uiState.value
        val session = current.currentSession
        val generatedPcmFilePath = session.generatedPcmFilePath
        persistGeneratedPlaybackDataForSavedAudio(
            result = result,
            session = session,
            enabled = current.isSavedAudioPlaybackDataStorageEnabled,
        )
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

    private suspend fun persistGeneratedPlaybackDataForSavedAudio(
        result: AudioExportResult.Success,
        session: ModeAudioSessionState,
        enabled: Boolean,
    ) {
        val metadata = session.generatedAudioMetadata ?: return
        val item = savedAudioRepository.listSavedAudio().findExportedItem(result) ?: return
        if (!enabled) {
            savedAudioDecodeCacheGateway.delete(item.itemId)
            return
        }
        val followDataForCache = completeFollowDataForSavedCache(session, metadata)
        if (!session.hasCompleteSavedPlaybackDataWorthCaching(metadata, item, followDataForCache)) {
            savedAudioDecodeCacheGateway.delete(item.itemId)
            safeDebugLog(
                "SavedAudioPlaybackDetails",
                "skipWrite itemId=${item.itemId} reason=incomplete-follow " +
                    "expectedPayloadBytes=${metadata.expectedPayloadByteCount(item) ?: -1} " +
                    "observedPayloadBytes=${followDataForCache.observedPayloadByteCount()}",
            )
            return
        }
        savedAudioDecodeCacheGateway.write(
            item = item,
            metadata = metadata,
            decodedPayload = session.decodedPayload,
            followData = followDataForCache,
            flashSignalInfo = session.generatedFlashSignalInfo,
        )
    }

    private suspend fun completeFollowDataForSavedCache(
        session: ModeAudioSessionState,
        metadata: GeneratedAudioMetadata,
    ): PayloadFollowViewData {
        val source = session.followWindowSource ?: return session.followData
        val expectedPayloadBytes = metadata.payloadByteCount.takeIf { it > 0 }
        if (expectedPayloadBytes != null && session.followData.observedPayloadByteCount() >= expectedPayloadBytes) {
            return session.followData
        }
        val gateway = audioCodecGateway ?: return session.followData
        return withContext(workerDispatcher) {
            val segments =
                source.segmentTexts.map { segmentText ->
                    gateway
                        .buildEncodeFollowData(
                            segmentText,
                            metadata.sampleRateHz,
                            metadata.frameSamples,
                            metadata.mode.nativeValue,
                            source.flashSignalProfile,
                            source.flashVoicingFlavor,
                        ).followData
                        .takeIf { it.followAvailable }
                        ?: return@withContext session.followData
                }
            mergeSegmentedFollowDataWindow(
                segments = segments,
                firstSampleOffset = 0,
                totalPcmSampleCount = source.totalPcmSampleCount,
            )
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

private fun List<SavedAudioItem>.findExportedItem(result: AudioExportResult.Success): SavedAudioItem? =
    firstOrNull { it.uriString == result.uriString }
        ?: firstOrNull { it.displayName == result.displayName }

private fun ModeAudioSessionState.hasCompleteSavedPlaybackDataWorthCaching(
    metadata: GeneratedAudioMetadata,
    item: SavedAudioItem,
    followDataForCache: PayloadFollowViewData = followData,
): Boolean {
    if (
        decodedPayload == DecodedPayloadViewData.Empty &&
        !followDataForCache.hasImmediatePlaybackData() &&
        !generatedFlashSignalInfo.available
    ) {
        return false
    }
    val expectedPayloadByteCount = metadata.expectedPayloadByteCount(item) ?: return followDataForCache.hasImmediatePlaybackData()
    return followDataForCache.followAvailable &&
        followDataForCache.observedPayloadByteCount() >= expectedPayloadByteCount
}

private fun PayloadFollowViewData.hasImmediatePlaybackData(): Boolean =
    followAvailable &&
        (
            textFollowAvailable ||
                lyricLineFollowAvailable ||
                byteTimeline.isNotEmpty() ||
                binaryGroupTimeline.isNotEmpty() ||
                ultraFrameTimeline.isNotEmpty()
        )

private fun GeneratedAudioMetadata.expectedPayloadByteCount(item: SavedAudioItem): Int? =
    payloadByteCount.takeIf { it > 0 }
        ?: item.payloadByteCount?.takeIf { it > 0 }

private fun PayloadFollowViewData.observedPayloadByteCount(): Int {
    val byteCountFromGroups =
        binaryGroupTimeline
            .maxOfOrNull { it.bitOffset + it.bitCount }
            ?.let { (it + 7) / 8 }
            ?: 0
    val byteCountFromRawUnits = textRawDisplayUnits.sumOf { it.byteCount.coerceAtLeast(1) }
    return listOf(
        byteTimeline.size,
        hexTokens.size,
        byteCountFromRawUnits,
        byteCountFromGroups,
    ).maxOrNull() ?: 0
}
