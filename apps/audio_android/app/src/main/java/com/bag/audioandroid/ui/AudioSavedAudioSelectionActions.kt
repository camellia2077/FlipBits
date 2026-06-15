package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.R
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.PlaybackRuntimeGateway
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.LibrarySelectionUiState
import com.bag.audioandroid.ui.state.PlaybackDetailsSource
import com.bag.audioandroid.ui.state.PlayerShellEvent
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import com.bag.audioandroid.ui.state.reduce
import com.bag.audioandroid.util.measureElapsedMs
import com.bag.audioandroid.util.safeDebugLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AudioSavedAudioSelectionActions(
    private val uiState: MutableStateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val playbackRuntimeGateway: PlaybackRuntimeGateway,
    private val savedAudioRepository: SavedAudioRepository,
    private val stopPlayback: () -> Unit,
    private val playCurrentFromStart: () -> Boolean,
    private val setCurrentStatusText: (UiText) -> Unit,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val savedAudioDecodeCacheGateway: SavedAudioDecodeCacheGateway,
    private val workerDispatcher: CoroutineDispatcher,
) {
    private var pendingSavedSelectionLoad: Job? = null

    fun onSavedAudioSelected(itemId: String) {
        releaseSavedDecodeLog("onSavedAudioSelected itemId=$itemId")
        stopPlayback()
        if (!prepareSavedAudioSelection(
                itemId,
                switchToAudioTab = false,
                clearLibrarySelection = true,
                closeSavedAudioSheet = false,
            )
        ) {
            releaseSavedDecodeLog("onSavedAudioSelected prepareFailed itemId=$itemId")
            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
            return
        }
        val savedAudio =
            uiState.value.selectedSavedAudio ?: run {
                releaseSavedDecodeLog("onSavedAudioSelected noSelection itemId=$itemId")
                return
            }
        releaseSavedDecodeLog(
            "onSavedAudioSelected prepared itemId=$itemId loading=${savedAudio.isLoadingContent} " +
                "needsDecoded=${savedAudio.needsDecodedContent} cachedTextStatus=${savedAudio.decodedPayload.textDecodeStatusCode} " +
                "followAvailable=${savedAudio.followData.textFollowAvailable}",
        )
        if (savedAudio.isLoadingContent) {
            return
        }
        setCurrentStatusText(
            UiText.Resource(
                R.string.status_saved_audio_loaded,
                listOf(savedAudio.item.displayName),
            ),
        )
    }

    fun prepareSavedAudioSelection(
        itemId: String,
        switchToAudioTab: Boolean = false,
        clearLibrarySelection: Boolean = false,
        closeSavedAudioSheet: Boolean = false,
    ): Boolean =
        prepareSavedAudioSelection(
            itemId = itemId,
            switchToAudioTab = switchToAudioTab,
            clearLibrarySelection = clearLibrarySelection,
            closeSavedAudioSheet = closeSavedAudioSheet,
            followUp = SavedAudioSelectionFollowUp.None,
        )

    fun prepareSavedAudioSelectionForPlayback(
        itemId: String,
        switchToAudioTab: Boolean = false,
        clearLibrarySelection: Boolean = false,
        closeSavedAudioSheet: Boolean = false,
    ): Boolean =
        prepareSavedAudioSelection(
            itemId = itemId,
            switchToAudioTab = switchToAudioTab,
            clearLibrarySelection = clearLibrarySelection,
            closeSavedAudioSheet = closeSavedAudioSheet,
            followUp = SavedAudioSelectionFollowUp.PlayFromStart,
        )

    private fun prepareSavedAudioSelection(
        itemId: String,
        switchToAudioTab: Boolean,
        clearLibrarySelection: Boolean,
        closeSavedAudioSheet: Boolean,
        followUp: SavedAudioSelectionFollowUp,
    ): Boolean {
        pendingSavedSelectionLoad?.cancel()
        val selectionStartedAtNs = System.nanoTime()
        val previousSelection = uiState.value.selectedSavedAudio
        releaseSavedDecodeLog(
            "selectionRequest itemId=$itemId previousItemId=${previousSelection?.item?.itemId.orEmpty()} " +
                "currentSource=${uiState.value.currentPlaybackSource} followUp=$followUp",
        )
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionRequest itemId=$itemId previousItemId=${previousSelection?.item?.itemId.orEmpty()} " +
                "currentSource=${uiState.value.currentPlaybackSource} " +
                "currentSaved=${uiState.value.selectedSavedAudio?.item?.itemId.orEmpty()}",
        )
        if (uiState.value.currentPlaybackSource != AudioPlaybackSource.Saved(itemId)) {
            safeDebugLog(
                SavedAudioPerfTag,
                "selectionRequestStop itemId=$itemId currentSource=${uiState.value.currentPlaybackSource}",
            )
            releaseSavedDecodeLog("selectionRequestStop itemId=$itemId currentSource=${uiState.value.currentPlaybackSource}")
            stopPlayback()
        }
        val placeholderItem =
            resolveSavedAudioItem(itemId) ?: run {
                releaseSavedDecodeLog("selectionResolveFailed itemId=$itemId")
                return false
            }
        releaseSavedDecodeLog(
            "selectionResolved itemId=$itemId durationMs=${placeholderItem.durationMs} " +
                "sampleRate=${placeholderItem.sampleRateHz ?: -1} async=${shouldHydrateSavedAudioAsync(placeholderItem)}",
        )
        if (shouldHydrateSavedAudioAsync(placeholderItem)) {
            releaseSavedDecodeLog("selectionAsyncPlaceholder itemId=$itemId")
            applySelection(
                savedAudio =
                    placeholderSelection(
                        item = placeholderItem,
                        previousSelection = previousSelection,
                    ),
                previousSelection = previousSelection,
                switchToAudioTab = switchToAudioTab,
                clearLibrarySelection = clearLibrarySelection,
                closeSavedAudioSheet = closeSavedAudioSheet,
                itemId = itemId,
                selectionStartedAtNs = selectionStartedAtNs,
            )
            pendingSavedSelectionLoad =
                scope.launch {
                    val (loadedSavedAudio, loadMs) =
                        withContext(workerDispatcher) {
                            measureElapsedMs { savedAudioRepository.loadSavedAudio(itemId) }
                        }
                    safeDebugLog(
                        SavedAudioPerfTag,
                        "selectionLoadSavedAudio itemId=$itemId elapsedMs=$loadMs loaded=${loadedSavedAudio != null} async=true",
                    )
                    releaseSavedDecodeLog(
                        "selectionLoadSavedAudio itemId=$itemId elapsedMs=$loadMs loaded=${loadedSavedAudio != null} async=true",
                    )
                    val hydrated =
                        loadedSavedAudio ?: run {
                            releaseSavedDecodeLog("selectionLoadFailed itemId=$itemId async=true")
                            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
                            return@launch
                        }
                    val (cachedDecode, cacheMs) =
                        withContext(workerDispatcher) {
                            measureElapsedMs { savedAudioDecodeCacheGateway.read(hydrated.item, hydrated.metadata) }
                        }
                    safeDebugLog(
                        SavedAudioPerfTag,
                        "selectionDecodeCache itemId=$itemId elapsedMs=$cacheMs hit=${cachedDecode != null} async=true",
                    )
                    releaseSavedDecodeLog(
                        "selectionDecodeCache itemId=$itemId elapsedMs=$cacheMs hit=${cachedDecode != null} async=true " +
                            "metadataMode=${hydrated.metadata?.mode?.wireName.orEmpty()} " +
                            "fileBacked=${!hydrated.pcmFilePath.isNullOrBlank()} " +
                            "pcmSamples=${hydrated.pcm.size} metadataSamples=${hydrated.metadata?.pcmSampleCount ?: 0}",
                    )
                    val (_, stateUpdateMs) =
                        measureElapsedMs {
                            uiState.update { state ->
                                val currentSelection =
                                    state.selectedSavedAudio
                                        ?.takeIf { it.item.itemId == itemId }
                                        ?: return@update state
                                state.copy(
                                    selectedSavedAudio =
                                        hydrated.toPlaybackSelection(
                                            playbackSpeed = currentSelection.playbackSpeed,
                                            cachedDecode = cachedDecode,
                                            playback =
                                                playbackRuntimeGateway.load(
                                                    hydrated.metadata?.pcmSampleCount ?: hydrated.pcm.size,
                                                    hydrated.sampleRateHz,
                                                ),
                                        ),
                                )
                            }
                        }
                    safeDebugLog(
                        SavedAudioPerfTag,
                        "selectionStateUpdate itemId=$itemId elapsedMs=$stateUpdateMs " +
                            "waveformSamples=${hydrated.waveformPcm.size} fileBacked=${!hydrated.pcmFilePath.isNullOrBlank()} async=true",
                    )
                    releaseSavedDecodeLog(
                        "selectionHydratedStateUpdate itemId=$itemId elapsedMs=$stateUpdateMs " +
                            "needsDecoded=${cachedDecode == null} waveformSamples=${hydrated.waveformPcm.size} " +
                            "fileBacked=${!hydrated.pcmFilePath.isNullOrBlank()} async=true " +
                            "detailsSource=${uiState.value.selectedSavedAudio?.playbackDetailsSource?.wireName.orEmpty()} " +
                            "${uiState.value.selectedSavedAudio?.followData?.selectionFollowDiagSummary().orEmpty()}",
                    )
                    val (_, pruneMs) =
                        measureElapsedMs {
                            generatedAudioCacheGateway.enforceGeneratedAudioCachePolicy(uiState.value)
                        }
                    safeDebugLog(
                        SavedAudioPerfTag,
                        "selectionCachePolicy itemId=$itemId elapsedMs=$pruneMs async=true",
                    )
                    safeDebugLog(
                        SavedAudioPerfTag,
                        "selectionHydrateEnd itemId=$itemId totalElapsedMs=${(System.nanoTime() - selectionStartedAtNs) / 1_000_000L}",
                    )
                    releaseSavedDecodeLog(
                        "selectionHydrateEnd itemId=$itemId totalElapsedMs=${(System.nanoTime() - selectionStartedAtNs) / 1_000_000L}",
                    )
                    setCurrentStatusText(
                        UiText.Resource(
                            R.string.status_saved_audio_loaded,
                            listOf(hydrated.item.displayName),
                        ),
                    )
                    runFollowUp(followUp)
                }
            return true
        }
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionStart itemId=$itemId previousItemId=${previousSelection?.item?.itemId.orEmpty()} " +
                "switchToAudioTab=$switchToAudioTab closeSheet=$closeSavedAudioSheet",
        )
        val (savedAudio, loadMs) = measureElapsedMs { savedAudioRepository.loadSavedAudio(itemId) }
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionLoadSavedAudio itemId=$itemId elapsedMs=$loadMs loaded=${savedAudio != null}",
        )
        releaseSavedDecodeLog("selectionLoadSavedAudio itemId=$itemId elapsedMs=$loadMs loaded=${savedAudio != null}")
        savedAudio ?: run {
            releaseSavedDecodeLog("selectionLoadFailed itemId=$itemId")
            return false
        }
        val (cachedDecode, cacheMs) = measureElapsedMs { savedAudioDecodeCacheGateway.read(savedAudio.item, savedAudio.metadata) }
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionDecodeCache itemId=$itemId elapsedMs=$cacheMs hit=${cachedDecode != null}",
        )
        releaseSavedDecodeLog(
            "selectionDecodeCache itemId=$itemId elapsedMs=$cacheMs hit=${cachedDecode != null} " +
                "metadataMode=${savedAudio.metadata?.mode?.wireName.orEmpty()} fileBacked=${!savedAudio.pcmFilePath.isNullOrBlank()} " +
                "pcmSamples=${savedAudio.pcm.size} metadataSamples=${savedAudio.metadata?.pcmSampleCount ?: 0}",
        )
        applySelection(
            savedAudio =
                savedAudio.toPlaybackSelection(
                    playbackSpeed =
                        previousSelection
                            ?.takeIf { it.item.itemId == savedAudio.item.itemId }
                            ?.playbackSpeed
                            ?: PlaybackSpeedOption.default.speed,
                    cachedDecode = cachedDecode,
                    playback =
                        playbackRuntimeGateway.load(
                            savedAudio.metadata?.pcmSampleCount ?: savedAudio.pcm.size,
                            savedAudio.sampleRateHz,
                        ),
                ),
            previousSelection = previousSelection,
            switchToAudioTab = switchToAudioTab,
            clearLibrarySelection = clearLibrarySelection,
            closeSavedAudioSheet = closeSavedAudioSheet,
            itemId = itemId,
            selectionStartedAtNs = selectionStartedAtNs,
        )
        runFollowUp(followUp)
        return true
    }

    fun onShellSavedAudioSelected(itemId: String) {
        releaseSavedDecodeLog("onShellSavedAudioSelected itemId=$itemId")
        stopPlayback()
        if (!prepareSavedAudioSelection(itemId, closeSavedAudioSheet = true)) {
            releaseSavedDecodeLog("onShellSavedAudioSelected prepareFailed itemId=$itemId")
            uiState.update { state ->
                state.copy(
                    playerShellState =
                        state.playerShellState.reduce(
                            PlayerShellEvent.SelectQueueItem(keepExpandedPlayer = state.isExpandedPlayerVisible),
                        ),
                )
            }
            setCurrentStatusText(UiText.Resource(R.string.status_saved_audio_load_failed))
            return
        }
        val savedAudio =
            uiState.value.selectedSavedAudio ?: run {
                releaseSavedDecodeLog("onShellSavedAudioSelected noSelection itemId=$itemId")
                return
            }
        releaseSavedDecodeLog(
            "onShellSavedAudioSelected prepared itemId=$itemId loading=${savedAudio.isLoadingContent} " +
                "needsDecoded=${savedAudio.needsDecodedContent} cachedTextStatus=${savedAudio.decodedPayload.textDecodeStatusCode} " +
                "followAvailable=${savedAudio.followData.textFollowAvailable}",
        )
        if (savedAudio.isLoadingContent) {
            return
        }
        setCurrentStatusText(
            UiText.Resource(
                R.string.status_saved_audio_loaded,
                listOf(savedAudio.item.displayName),
            ),
        )
    }

    fun onEnterLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (state.savedAudioItems.none { it.itemId == itemId }) {
                return@update state
            }
            state.copy(
                librarySelection =
                    LibrarySelectionUiState(
                        isSelectionMode = true,
                        selectedItemIds = setOf(itemId),
                    ),
            )
        }
    }

    fun onToggleLibrarySelection(itemId: String) {
        uiState.update { state ->
            if (!state.librarySelection.isSelectionMode ||
                state.savedAudioItems.none { it.itemId == itemId }
            ) {
                return@update state
            }

            val updatedSelection =
                if (itemId in state.librarySelection.selectedItemIds) {
                    state.librarySelection.selectedItemIds - itemId
                } else {
                    state.librarySelection.selectedItemIds + itemId
                }
            state.copy(
                librarySelection =
                    if (updatedSelection.isEmpty()) {
                        LibrarySelectionUiState()
                    } else {
                        state.librarySelection.copy(selectedItemIds = updatedSelection)
                    },
            )
        }
    }

    fun onSelectAllLibraryItems(itemIds: Collection<String>? = null) {
        uiState.update { state ->
            val allItemIds =
                itemIds
                    ?.filter { candidateId -> state.savedAudioItems.any { it.itemId == candidateId } }
                    ?.toSet()
                    ?: state.savedAudioItems.map { it.itemId }.toSet()
            if (allItemIds.isEmpty()) {
                state
            } else {
                state.copy(
                    librarySelection =
                        LibrarySelectionUiState(
                            isSelectionMode = true,
                            selectedItemIds = allItemIds,
                        ),
                )
            }
        }
    }

    fun onClearLibrarySelection() {
        uiState.update { it.copy(librarySelection = LibrarySelectionUiState()) }
    }

    private fun applySelection(
        savedAudio: SavedAudioPlaybackSelection,
        previousSelection: SavedAudioPlaybackSelection?,
        switchToAudioTab: Boolean,
        clearLibrarySelection: Boolean,
        closeSavedAudioSheet: Boolean,
        itemId: String,
        selectionStartedAtNs: Long,
    ) {
        if (previousSelection?.item?.itemId != savedAudio.item.itemId) {
            safeDebugLog(
                SavedAudioPerfTag,
                "selectionDisposePrevious previousItemId=${previousSelection?.item?.itemId.orEmpty()} " +
                    "previousFile=${previousSelection?.pcmFilePath.orEmpty()} nextItemId=${savedAudio.item.itemId}",
            )
            generatedAudioCacheGateway.deleteCachedFile(previousSelection?.pcmFilePath)
        }
        val (_, stateUpdateMs) =
            measureElapsedMs {
                uiState.update { state ->
                    state.copy(
                        selectedTab = if (switchToAudioTab) AppTab.Data else state.selectedTab,
                        playerShellState =
                            if (closeSavedAudioSheet || switchToAudioTab) {
                                state.playerShellState.reduce(
                                    PlayerShellEvent.SelectQueueItem(keepExpandedPlayer = state.isExpandedPlayerVisible),
                                )
                            } else {
                                state.playerShellState
                            },
                        currentPlaybackSource = AudioPlaybackSource.Saved(savedAudio.item.itemId),
                        selectedSavedAudio = savedAudio,
                        librarySelection =
                            if (clearLibrarySelection) {
                                LibrarySelectionUiState()
                            } else {
                                state.librarySelection
                            },
                    )
                }
            }
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionStateUpdate itemId=$itemId elapsedMs=$stateUpdateMs " +
                "waveformSamples=${savedAudio.waveformPcm.size} fileBacked=${!savedAudio.pcmFilePath.isNullOrBlank()} " +
                "loading=${savedAudio.isLoadingContent} previousItemId=${previousSelection?.item?.itemId.orEmpty()} " +
                "currentSource=${uiState.value.currentPlaybackSource}",
        )
        releaseSavedDecodeLog(
            "selectionStateUpdate itemId=$itemId elapsedMs=$stateUpdateMs " +
                "loading=${savedAudio.isLoadingContent} needsDecoded=${savedAudio.needsDecodedContent} " +
                "decoding=${savedAudio.isDecodingContent} fileBacked=${!savedAudio.pcmFilePath.isNullOrBlank()} " +
                "currentSource=${uiState.value.currentPlaybackSource} " +
                "detailsSource=${savedAudio.playbackDetailsSource.wireName} ${savedAudio.followData.selectionFollowDiagSummary()}",
        )
        val (_, pruneMs) =
            measureElapsedMs {
                generatedAudioCacheGateway.enforceGeneratedAudioCachePolicy(uiState.value)
            }
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionCachePolicy itemId=$itemId elapsedMs=$pruneMs loading=${savedAudio.isLoadingContent}",
        )
        safeDebugLog(
            SavedAudioPerfTag,
            "selectionEnd itemId=$itemId totalElapsedMs=${(System.nanoTime() - selectionStartedAtNs) / 1_000_000L} " +
                "loading=${savedAudio.isLoadingContent}",
        )
        releaseSavedDecodeLog(
            "selectionEnd itemId=$itemId totalElapsedMs=${(System.nanoTime() - selectionStartedAtNs) / 1_000_000L} " +
                "loading=${savedAudio.isLoadingContent} needsDecoded=${savedAudio.needsDecodedContent} " +
                "detailsSource=${savedAudio.playbackDetailsSource.wireName}",
        )
    }

    private fun resolveSavedAudioItem(itemId: String) =
        uiState.value.savedAudioItems.firstOrNull { it.itemId == itemId }
            ?: savedAudioRepository.listSavedAudio().firstOrNull { it.itemId == itemId }

    private fun shouldHydrateSavedAudioAsync(item: com.bag.audioandroid.domain.SavedAudioItem): Boolean =
        item.durationMs >= LONG_AUDIO_FILE_THRESHOLD_MS

    private fun placeholderSelection(
        item: com.bag.audioandroid.domain.SavedAudioItem,
        previousSelection: SavedAudioPlaybackSelection?,
    ): SavedAudioPlaybackSelection {
        val sampleRateHz = item.sampleRateHz?.takeIf { it > 0 } ?: DEFAULT_SAVED_AUDIO_SAMPLE_RATE_HZ
        val estimatedTotalSamples = ((item.durationMs * sampleRateHz.toLong()) / 1_000L).toInt().coerceAtLeast(0)
        return SavedAudioPlaybackSelection(
            item = item,
            pcm = shortArrayOf(),
            waveformPcm = shortArrayOf(),
            pcmFilePath = null,
            sampleRateHz = sampleRateHz,
            metadata = null,
            wavAudioInfo = com.bag.audioandroid.domain.WavAudioInfo.Empty,
            playback = playbackRuntimeGateway.load(estimatedTotalSamples, sampleRateHz),
            playbackSpeed =
                previousSelection
                    ?.takeIf { it.item.itemId == item.itemId }
                    ?.playbackSpeed
                    ?: PlaybackSpeedOption.default.speed,
            isLoadingContent = true,
            playbackDetailsSource = PlaybackDetailsSource.Loading,
        )
    }

    private fun runFollowUp(followUp: SavedAudioSelectionFollowUp) {
        when (followUp) {
            SavedAudioSelectionFollowUp.None -> Unit
            SavedAudioSelectionFollowUp.PlayFromStart -> playCurrentFromStart()
        }
    }
}

private const val SavedAudioPerfTag = "SavedAudioPerf"
private const val SavedAudioDecodeProgressTag = "SavedAudioDecodeProgress"
private const val LONG_AUDIO_FILE_THRESHOLD_MS = 120_000L
private const val DEFAULT_SAVED_AUDIO_SAMPLE_RATE_HZ = 44_100

private fun releaseSavedDecodeLog(message: String) {
    try {
        Log.e(SavedAudioDecodeProgressTag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.e is not implemented.
    }
}

private enum class SavedAudioSelectionFollowUp {
    None,
    PlayFromStart,
}

private fun PayloadFollowViewData.selectionFollowDiagSummary(): String =
    "follow=$followAvailable textFollow=$textFollowAvailable tokens=${textTokens.size} " +
        "textTimeline=${textTokenTimeline.size} rawUnits=${textRawDisplayUnits.size} " +
        "binaryGroups=${binaryGroupTimeline.size} ultraFrames=${ultraFrameTimeline.size} total=$totalPcmSampleCount"
