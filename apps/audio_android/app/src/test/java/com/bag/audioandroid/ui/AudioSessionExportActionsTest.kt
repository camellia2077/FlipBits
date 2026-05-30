package com.bag.audioandroid.ui

import com.bag.audioandroid.R
import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.DecodeProgressUpdate
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioDecodedCacheEntry
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.FlashVoicingStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.UiText
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.FollowDataWindowSource
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioSessionExportActionsTest {
    @Test
    fun `export success updates status and emits snackbar`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult =
                            AudioExportResult.Success(
                                displayName = "test.wav",
                                uriString = "content://saved/test",
                            ),
                    ),
                savedAudioDecodeCacheGateway = ExportFakeSavedAudioDecodeCacheGateway(),
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertResId(state.value.currentSession.statusText, R.string.status_audio_saved)
        assertResId(
            state.value.snackbarMessage?.text ?: UiText.Empty,
            R.string.snackbar_audio_saved_to_library,
        )
        assertNotNull(state.value.snackbarMessage?.id)
    }

    @Test
    fun `export failure emits failure snackbar`() {
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository = FakeExportRepository(exportResult = AudioExportResult.Failed),
                savedAudioDecodeCacheGateway = ExportFakeSavedAudioDecodeCacheGateway(),
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertResId(state.value.currentSession.statusText, R.string.status_audio_save_failed)
        assertResId(
            state.value.snackbarMessage?.text ?: UiText.Empty,
            R.string.snackbar_audio_save_failed,
        )
    }

    @Test
    fun `export success deletes file backed cache and clears generated file path`() {
        val cacheGateway = TrackingExportGeneratedAudioCacheGateway()
        val fileBackedPath = "C:/tmp/generated-audio/flash_cached.pcm16"
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                sessionWithGeneratedAudio().copy(
                                    generatedPcm = shortArrayOf(),
                                    generatedPcmFilePath = fileBackedPath,
                                ),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult =
                            AudioExportResult.Success(
                                displayName = "saved.wav",
                                uriString = "content://saved/test",
                            ),
                    ),
                savedAudioDecodeCacheGateway = ExportFakeSavedAudioDecodeCacheGateway(),
                generatedAudioCacheGateway = cacheGateway,
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertNull(state.value.currentSession.generatedPcmFilePath)
        assertEquals(listOf(fileBackedPath), cacheGateway.deletedPaths)
        assertEquals(listOf(emptySet<String>()), cacheGateway.prunedRetainedPaths)
    }

    @Test
    fun `share generated audio does not export to library and opens share path directly`() {
        val repository = FakeExportRepository(exportResult = AudioExportResult.Failed, shareGeneratedResult = true)
        var refreshCount = 0
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository = repository,
                savedAudioDecodeCacheGateway = ExportFakeSavedAudioDecodeCacheGateway(),
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = { refreshCount += 1 },
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onShareCurrentGeneratedAudio()

        assertEquals(0, repository.exportGeneratedAudioCalls)
        assertEquals(1, repository.shareGeneratedAudioCalls)
        assertEquals(0, refreshCount)
        assertNull(state.value.snackbarMessage)
    }

    @Test
    fun `share generated audio failure emits failure snackbar`() {
        val repository = FakeExportRepository(exportResult = AudioExportResult.Failed, shareGeneratedResult = false)
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to sessionWithGeneratedAudio(),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository = repository,
                savedAudioDecodeCacheGateway = ExportFakeSavedAudioDecodeCacheGateway(),
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onShareCurrentGeneratedAudio()

        assertEquals(0, repository.exportGeneratedAudioCalls)
        assertEquals(1, repository.shareGeneratedAudioCalls)
        assertResId(
            state.value.snackbarMessage?.text ?: UiText.Empty,
            R.string.snackbar_audio_save_failed,
        )
    }

    @Test
    fun `export success stores generated decode data for saved audio when enabled`() {
        val item =
            savedAudioItem(
                itemId = "saved-1",
                displayName = "saved.wav",
                uriString = "content://saved/test",
            )
        val decodeCache = ExportFakeSavedAudioDecodeCacheGateway()
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    isSavedAudioPlaybackDataStorageEnabled = true,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                sessionWithGeneratedAudio().copy(
                                    decodedPayload = DecodedPayloadViewData(text = "decoded", rawPayloadAvailable = true),
                                    followData = completeFollowData(payloadByteCount = 4),
                                    generatedFlashSignalInfo = FlashSignalInfo(available = true),
                                ),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult = AudioExportResult.Success(item.displayName, item.uriString),
                        listedItems = listOf(item),
                    ),
                savedAudioDecodeCacheGateway = decodeCache,
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertEquals(listOf(item.itemId), decodeCache.writtenItemIds)
        assertTrue(decodeCache.deletedItemIds.isEmpty())
    }

    @Test
    fun `export success skips partial generated follow window cache`() {
        val item =
            savedAudioItem(
                itemId = "saved-1",
                displayName = "saved.wav",
                uriString = "content://saved/test",
            )
        val decodeCache = ExportFakeSavedAudioDecodeCacheGateway()
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    isSavedAudioPlaybackDataStorageEnabled = true,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                sessionWithGeneratedAudio().copy(
                                    decodedPayload = DecodedPayloadViewData(text = "decoded", rawPayloadAvailable = true),
                                    followData = completeFollowData(payloadByteCount = 2),
                                    generatedFlashSignalInfo = FlashSignalInfo(available = true),
                                ),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult = AudioExportResult.Success(item.displayName, item.uriString),
                        listedItems = listOf(item),
                    ),
                savedAudioDecodeCacheGateway = decodeCache,
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertTrue(decodeCache.writtenItemIds.isEmpty())
        assertEquals(listOf(item.itemId), decodeCache.deletedItemIds)
    }

    @Test
    fun `export success stores complete generated follow cache from window source`() {
        val item =
            savedAudioItem(
                itemId = "saved-1",
                displayName = "saved.wav",
                uriString = "content://saved/test",
            )
        val decodeCache = ExportFakeSavedAudioDecodeCacheGateway()
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    isSavedAudioPlaybackDataStorageEnabled = true,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                sessionWithGeneratedAudio().copy(
                                    decodedPayload = DecodedPayloadViewData(text = "decoded", rawPayloadAvailable = true),
                                    followData = completeFollowData(payloadByteCount = 2),
                                    followWindowSource =
                                        FollowDataWindowSource(
                                            segmentTexts = listOf("aa", "bb"),
                                            segmentSampleCounts = listOf(20, 20),
                                            totalPcmSampleCount = 40,
                                        ),
                                ),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult = AudioExportResult.Success(item.displayName, item.uriString),
                        listedItems = listOf(item),
                    ),
                savedAudioDecodeCacheGateway = decodeCache,
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                audioCodecGateway = ExportFakeAudioCodecGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertEquals(listOf(item.itemId), decodeCache.writtenItemIds)
        assertEquals(
            4,
            decodeCache
                .writtenFollowData
                .single()
                .byteTimeline
                .size,
        )
        assertTrue(decodeCache.deletedItemIds.isEmpty())
    }

    @Test
    fun `export success does not store generated decode data when disabled`() {
        val item =
            savedAudioItem(
                itemId = "saved-1",
                displayName = "saved.wav",
                uriString = "content://saved/test",
            )
        val decodeCache = ExportFakeSavedAudioDecodeCacheGateway()
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    isSavedAudioPlaybackDataStorageEnabled = false,
                    sessions =
                        mapOf(
                            TransportModeOption.Flash to
                                sessionWithGeneratedAudio().copy(
                                    decodedPayload = DecodedPayloadViewData(text = "decoded", rawPayloadAvailable = true),
                                    followData = PayloadFollowViewData(followAvailable = true, textFollowAvailable = true),
                                ),
                        ),
                ),
            )
        val actions =
            AudioSessionExportActions(
                uiState = state,
                scope = CoroutineScope(Dispatchers.Unconfined),
                sessionStateStore = AudioSessionStateStore(state),
                savedAudioRepository =
                    FakeExportRepository(
                        exportResult = AudioExportResult.Success(item.displayName, item.uriString),
                        listedItems = listOf(item),
                    ),
                savedAudioDecodeCacheGateway = decodeCache,
                generatedAudioCacheGateway = ExportFakeGeneratedAudioCacheGateway(),
                sampleRateHz = 44100,
                refreshSavedAudioItems = {},
                workerDispatcher = Dispatchers.Unconfined,
            )

        actions.onExportAudio()

        assertTrue(decodeCache.writtenItemIds.isEmpty())
        assertEquals(listOf(item.itemId), decodeCache.deletedItemIds)
    }

    private fun sessionWithGeneratedAudio() =
        ModeAudioSessionState(
            inputText = "text",
            generatedPcm = shortArrayOf(1, 2, 3),
            generatedAudioMetadata =
                GeneratedAudioMetadata(
                    mode = TransportModeOption.Flash,
                    flashVoicingStyle = FlashVoicingStyleOption.Standard,
                    createdAtIsoUtc = "2026-03-17T00:00:00Z",
                    durationMs = 1L,
                    sampleRateHz = 44_100,
                    frameSamples = 2205,
                    pcmSampleCount = 3,
                    payloadByteCount = 4,
                    inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                    appVersion = "1.0.0",
                    coreVersion = "1.0.0",
                ),
            generatedFlashVoicingStyle = FlashVoicingStyleOption.Standard,
        )

    private fun assertResId(
        text: UiText,
        expectedResId: Int,
    ) {
        assertTrue(text is UiText.Resource)
        assertEquals(expectedResId, (text as UiText.Resource).resId)
    }
}

private fun completeFollowData(payloadByteCount: Int): PayloadFollowViewData =
    PayloadFollowViewData(
        followAvailable = true,
        textFollowAvailable = true,
        byteTimeline =
            List(payloadByteCount) { index ->
                PayloadFollowByteTimelineEntry(
                    startSample = index * 10,
                    sampleCount = 10,
                    byteIndex = index,
                )
            },
        binaryGroupTimeline =
            List(payloadByteCount * 8) { index ->
                PayloadFollowBinaryGroupTimelineEntry(
                    startSample = index,
                    sampleCount = 1,
                    groupIndex = index,
                    bitOffset = index,
                    bitCount = 1,
                )
            },
    )

private class FakeExportRepository(
    private val exportResult: AudioExportResult,
    private val listedItems: List<SavedAudioItem> = emptyList(),
    private val shareGeneratedResult: Boolean = false,
) : SavedAudioRepository {
    var exportGeneratedAudioCalls = 0
    var shareGeneratedAudioCalls = 0

    override fun suggestGeneratedAudioDisplayName(
        inputText: String,
        metadata: GeneratedAudioMetadata,
    ): String = "test.wav"

    override fun exportGeneratedAudio(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): AudioExportResult {
        exportGeneratedAudioCalls += 1
        return exportResult
    }

    override fun exportGeneratedAudioToDocument(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
        destinationUriString: String,
    ): Boolean = false

    override fun listSavedAudio(): List<SavedAudioItem> = listedItems

    override fun loadSavedAudio(itemId: String): SavedAudioContent? = null

    override fun deleteSavedAudio(itemId: String): Boolean = false

    override fun renameSavedAudio(
        itemId: String,
        newBaseName: String,
    ): SavedAudioRenameResult = SavedAudioRenameResult.Failed

    override fun importAudio(uriString: String): SavedAudioImportResult = SavedAudioImportResult.Failed

    override fun exportSavedAudioToDocument(
        itemId: String,
        destinationUriString: String,
    ): Boolean = false

    override fun shareSavedAudio(item: SavedAudioItem): Boolean = false

    override fun shareGeneratedAudio(
        inputText: String,
        pcm: ShortArray,
        pcmFilePath: String?,
        sampleRateHz: Int,
        metadata: GeneratedAudioMetadata,
    ): Boolean {
        shareGeneratedAudioCalls += 1
        return shareGeneratedResult
    }
}

private class ExportFakeSavedAudioDecodeCacheGateway : SavedAudioDecodeCacheGateway {
    val writtenItemIds = mutableListOf<String>()
    val writtenFollowData = mutableListOf<PayloadFollowViewData>()
    val deletedItemIds = mutableListOf<String>()

    override fun exists(itemId: String): Boolean = itemId in writtenItemIds

    override fun read(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ): SavedAudioDecodedCacheEntry? = null

    override fun write(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
        decodedPayload: DecodedPayloadViewData,
        followData: PayloadFollowViewData,
        flashSignalInfo: FlashSignalInfo,
    ) {
        writtenItemIds += item.itemId
        writtenFollowData += followData
    }

    override fun delete(itemId: String) {
        deletedItemIds += itemId
    }

    override fun prune(validItemIds: Set<String>) = Unit
}

private class ExportFakeAudioCodecGateway : AudioCodecGateway {
    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int = 0

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit,
    ): EncodeAudioResult = EncodeAudioResult.Cancelled

    override suspend fun buildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult =
        EncodedAudioPayloadResult(
            followData = completeFollowData(payloadByteCount = text.length),
        )

    override fun describeFlashSignal(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): FlashSignalInfo = FlashSignalInfo.Empty

    override fun validateDecodeConfig(
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int = 0

    override suspend fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodedAudioPayloadResult = DecodedAudioPayloadResult()

    override suspend fun decodePcmFileSegment(
        pcmFilePath: String,
        startSample: Long,
        sampleCount: Int,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult = DecodedAudioPayloadResult()

    override fun getCoreVersion(): String = "test"
}

private fun savedAudioItem(
    itemId: String,
    displayName: String,
    uriString: String,
): SavedAudioItem =
    SavedAudioItem(
        itemId = itemId,
        displayName = displayName,
        uriString = uriString,
        modeWireName = TransportModeOption.Flash.wireName,
        durationMs = 1L,
        savedAtEpochSeconds = 1L,
        flashVoicingStyle = FlashVoicingStyleOption.Standard,
        sampleRateHz = 44100,
        payloadByteCount = 4,
    )

private class ExportFakeGeneratedAudioCacheGateway : GeneratedAudioCacheGateway {
    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter =
        object : GeneratedAudioPcmCacheWriter {
            override val filePath: String = File.createTempFile("${modeWireName}_", ".pcm16").absolutePath

            override fun appendPcm(pcm: ShortArray) = Unit

            override fun finish() = Unit

            override fun abort() = Unit
        }

    override fun deleteCachedFile(path: String?) = Unit

    override fun pruneCachedFiles(retainedPaths: Set<String>) = Unit
}

private class TrackingExportGeneratedAudioCacheGateway : GeneratedAudioCacheGateway {
    val deletedPaths = mutableListOf<String>()
    val prunedRetainedPaths = mutableListOf<Set<String>>()

    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter =
        object : GeneratedAudioPcmCacheWriter {
            override val filePath: String = File.createTempFile("${modeWireName}_", ".pcm16").absolutePath

            override fun appendPcm(pcm: ShortArray) = Unit

            override fun finish() = Unit

            override fun abort() = Unit
        }

    override fun deleteCachedFile(path: String?) {
        if (path != null) {
            deletedPaths += path
        }
    }

    override fun pruneCachedFiles(retainedPaths: Set<String>) {
        prunedRetainedPaths += retainedPaths
    }
}
