package com.bag.audioandroid.ui

import com.bag.audioandroid.domain.AudioCodecGateway
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.BagDecodeContentCodes
import com.bag.audioandroid.domain.DecodeProgressUpdate
import com.bag.audioandroid.domain.DecodedAudioPayloadResult
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.EncodeAudioResult
import com.bag.audioandroid.domain.EncodeProgressUpdate
import com.bag.audioandroid.domain.EncodedAudioPayloadResult
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioDecodedCacheEntry
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.PlaybackUiState
import com.bag.audioandroid.ui.state.SavedAudioPlaybackSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionDecodeFileBackedActionsTest {
    @Test
    fun `decode uses codec gateway for file backed saved audio segments`() =
        runTest {
            val gateway =
                RecordingFileSegmentCodecGateway(
                    decodeResultsQueue =
                        ArrayDeque(
                            listOf(
                                decodedSegment("file-1 ", 2),
                                decodedSegment("file-2", 3),
                            ),
                        ),
                )
            val dispatcher = StandardTestDispatcher(testScheduler)
            val uiState = MutableStateFlow(AudioAppUiState())
            val sessionStateStore = AudioSessionStateStore(uiState)
            val actions =
                AudioSessionDecodeActions(
                    uiState = uiState,
                    scope = CoroutineScope(dispatcher),
                    audioCodecGateway = gateway,
                    sessionStateStore = sessionStateStore,
                    uiTextMapper = BagUiTextMapper(),
                    sampleRateHz = 44_100,
                    frameSamples = 2_205,
                    workerDispatcher = dispatcher,
                    savedAudioDecodeCacheGateway = EmptySavedAudioDecodeCacheGateway,
                )
            val pcmFilePath = "C:/tmp/saved-file-backed.pcm16"

            uiState.value =
                uiState.value.copy(
                    selectedSavedAudio = fileBackedSavedAudio(pcmFilePath),
                    currentPlaybackSource = AudioPlaybackSource.Saved("saved-file"),
                )

            actions.onDecode()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FileSegmentDecodeCall(pcmFilePath = pcmFilePath, startSample = 0, sampleCount = 2),
                    FileSegmentDecodeCall(pcmFilePath = pcmFilePath, startSample = 2, sampleCount = 3),
                ),
                gateway.fileSegmentDecodeCalls,
            )
            val selected = uiState.value.selectedSavedAudio ?: error("selected audio missing")
            assertEquals("file-1 file-2", selected.decodedPayload.text)
            assertEquals(5, selected.followData.totalPcmSampleCount)
        }
}

private fun decodedSegment(
    text: String,
    sampleCount: Int,
): DecodedAudioPayloadResult =
    DecodedAudioPayloadResult(
        decodedPayload =
            DecodedPayloadViewData(
                text = text,
                rawBytesHex = text.encodeToByteArray().joinToString(separator = " ") { "%02X".format(it) },
                rawBitsBinary = "01100110",
                textDecodeStatusCode = BagDecodeContentCodes.STATUS_OK,
                rawPayloadAvailable = true,
            ),
        followData =
            PayloadFollowViewData(
                payloadBeginSample = 0,
                payloadSampleCount = sampleCount,
                totalPcmSampleCount = sampleCount,
                followAvailable = true,
            ),
    )

private fun fileBackedSavedAudio(pcmFilePath: String): SavedAudioPlaybackSelection =
    SavedAudioPlaybackSelection(
        item =
            SavedAudioItem(
                itemId = "saved-file",
                displayName = "Saved File",
                uriString = "content://saved/file",
                modeWireName = TransportModeOption.Ultra.wireName,
                durationMs = 1000,
                savedAtEpochSeconds = 0,
            ),
        pcm = shortArrayOf(),
        pcmFilePath = pcmFilePath,
        sampleRateHz = 44_100,
        metadata =
            GeneratedAudioMetadata(
                mode = TransportModeOption.Ultra,
                createdAtIsoUtc = "2026-04-27T00:00:00Z",
                durationMs = 1000,
                sampleRateHz = 44_100,
                frameSamples = 2205,
                pcmSampleCount = 5,
                payloadByteCount = 12,
                inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                segmentCount = 2,
                appVersion = "test",
                coreVersion = "test",
                segmentSampleCounts = listOf(2, 3),
            ),
        playback = PlaybackUiState(),
    )

private class RecordingFileSegmentCodecGateway(
    private val decodeResultsQueue: ArrayDeque<DecodedAudioPayloadResult>,
) : AudioCodecGateway {
    val fileSegmentDecodeCalls = mutableListOf<FileSegmentDecodeCall>()

    override fun validateEncodeRequest(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): Int = BagApiCodes.VALIDATION_OK

    override suspend fun encodeTextToPcm(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (EncodeProgressUpdate) -> Unit,
    ): EncodeAudioResult = throw UnsupportedOperationException()

    override suspend fun buildEncodeFollowData(
        text: String,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): EncodedAudioPayloadResult = throw UnsupportedOperationException()

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
    ): Int = BagApiCodes.VALIDATION_OK

    override suspend fun decodeGeneratedPcm(
        pcm: ShortArray,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
        onProgress: (DecodeProgressUpdate) -> Unit,
    ): DecodedAudioPayloadResult = throw UnsupportedOperationException()

    override suspend fun decodePcmFileSegment(
        pcmFilePath: String,
        startSample: Long,
        sampleCount: Int,
        sampleRateHz: Int,
        frameSamples: Int,
        mode: Int,
        flashSignalProfile: Int,
        flashVoicingFlavor: Int,
    ): DecodedAudioPayloadResult {
        fileSegmentDecodeCalls +=
            FileSegmentDecodeCall(
                pcmFilePath = pcmFilePath,
                startSample = startSample,
                sampleCount = sampleCount,
            )
        return decodeResultsQueue.removeFirst()
    }

    override fun getCoreVersion(): String = "test"
}

private data class FileSegmentDecodeCall(
    val pcmFilePath: String,
    val startSample: Long,
    val sampleCount: Int,
)

private object EmptySavedAudioDecodeCacheGateway : SavedAudioDecodeCacheGateway {
    override fun exists(itemId: String): Boolean = false

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
    ) = Unit

    override fun delete(itemId: String) = Unit

    override fun prune(validItemIds: Set<String>) = Unit
}
