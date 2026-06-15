package com.bag.audioandroid.ui

import com.bag.audioandroid.audio.VoicePreviewPlayer
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.BagApiCodes
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.ImportedVoiceAudio
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryMetadata
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.domain.VoiceAudioFileGateway
import com.bag.audioandroid.domain.VoiceAudioImportResult
import com.bag.audioandroid.domain.VoiceFxGateway
import com.bag.audioandroid.domain.VoiceFxProcessResult
import com.bag.audioandroid.domain.VoiceFxProcessorSession
import com.bag.audioandroid.domain.VoiceLiveConfig
import com.bag.audioandroid.domain.VoiceLiveGateway
import com.bag.audioandroid.domain.VoiceLiveRouteSnapshot
import com.bag.audioandroid.domain.VoiceRecordingGateway
import com.bag.audioandroid.ui.model.VoiceFxPresetOption
import com.bag.audioandroid.ui.model.VoiceFxSubvoiceStyleOption
import com.bag.audioandroid.ui.model.VoiceInputSourceOption
import com.bag.audioandroid.ui.model.VoicePreviewTrackOption
import com.bag.audioandroid.ui.model.VoiceRecordProcessingModeOption
import com.bag.audioandroid.ui.model.VoiceTrackModeOption
import com.bag.audioandroid.ui.model.VoiceWorkflowModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.AudioDocumentExportSource
import com.bag.audioandroid.ui.state.VoiceSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class VoiceSessionActionsTest {
    @Test
    fun presetSelectionClearsProcessedStateAndStopsPreview() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 2, 3),
                                processedPcm = shortArrayOf(4, 5, 6),
                                debugMainVoicePcm = shortArrayOf(7),
                                debugSubvoicePcm = shortArrayOf(8),
                                isPreviewPlaying = true,
                                selectedPreset = VoiceFxPresetOption.MachineVoice,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onPresetSelected(VoiceFxPresetOption.Binharic)

            val session = uiState.value.voiceSession
            assertEquals(VoiceFxPresetOption.Binharic, session.selectedPreset)
            assertEquals(VoiceTrackModeOption.Dual, session.selectedTrackMode)
            assertTrue(session.inputPcm.isNotEmpty())
            assertTrue(session.processedPcm.isEmpty())
            assertTrue(session.debugMainVoicePcm.isEmpty())
            assertTrue(session.debugSubvoicePcm.isEmpty())
            assertFalse(session.isPreviewPlaying)
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
        }

    @Test
    fun processRecordingStoresFinalMixAndDebugTracks() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(11, 12, 13, 14),
                                sampleRateHz = 44100,
                                selectedPreset = VoiceFxPresetOption.Binharic,
                            ),
                    ),
                )
            val gateway =
                FakeVoiceFxGateway(
                    result =
                        VoiceFxProcessResult.Success(
                            finalMix = shortArrayOf(21, 22, 23, 24),
                            mainVoice = shortArrayOf(31, 32, 33, 34),
                            subvoice = shortArrayOf(41, 42, 43, 44),
                            signalOverlay = shortArrayOf(),
                        ),
                )
            var stopGlobalPlaybackCalls = 0
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = gateway,
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = { stopGlobalPlaybackCalls += 1 },
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onProcessRecording()
            advanceUntilIdle()

            val session = uiState.value.voiceSession
            assertFalse(session.isProcessing)
            assertArrayEquals(shortArrayOf(21, 22, 23, 24), session.processedPcm)
            assertArrayEquals(shortArrayOf(31, 32, 33, 34), session.debugMainVoicePcm)
            assertArrayEquals(shortArrayOf(41, 42, 43, 44), session.debugSubvoicePcm)
            assertTrue(session.debugSignalOverlayPcm.isEmpty())
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
            assertEquals(1, stopGlobalPlaybackCalls)
        }

    @Test
    fun subvoiceStyleSelectionClearsProcessedState() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 2, 3),
                                processedPcm = shortArrayOf(4, 5, 6),
                                debugSubvoicePcm = shortArrayOf(7, 8),
                                selectedPreset = VoiceFxPresetOption.Binharic,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onSubvoiceStyleSelected(VoiceFxSubvoiceStyleOption.Litany)

            val session = uiState.value.voiceSession
            assertEquals(VoiceFxSubvoiceStyleOption.Litany, session.selectedSubvoiceStyle)
            assertTrue(session.processedPcm.isEmpty())
            assertTrue(session.debugSubvoicePcm.isEmpty())
        }

    @Test
    fun trackModeSelectionSwitchesToCompatiblePresetAndClearsProcessedState() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 2, 3),
                                processedPcm = shortArrayOf(4, 5, 6),
                                debugSubvoicePcm = shortArrayOf(7, 8),
                                selectedTrackMode = VoiceTrackModeOption.Single,
                                selectedPreset = VoiceFxPresetOption.MachineVoice,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onTrackModeSelected(VoiceTrackModeOption.Dual)

            val session = uiState.value.voiceSession
            assertEquals(VoiceTrackModeOption.Dual, session.selectedTrackMode)
            assertEquals(VoiceFxPresetOption.Binharic, session.selectedPreset)
            assertTrue(session.processedPcm.isEmpty())
            assertTrue(session.debugSubvoicePcm.isEmpty())
        }

    @Test
    fun processSignalCantStoresOverlayDiagnostics() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 3, 5, 7),
                                sampleRateHz = 44100,
                                selectedPreset = VoiceFxPresetOption.SignalCant,
                            ),
                    ),
                )
            val gateway =
                FakeVoiceFxGateway(
                    result =
                        VoiceFxProcessResult.Success(
                            finalMix = shortArrayOf(9, 8, 7, 6),
                            mainVoice = shortArrayOf(6, 5, 4, 3),
                            subvoice = shortArrayOf(),
                            signalOverlay = shortArrayOf(2, 0, -2, 0),
                        ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = gateway,
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onProcessRecording()
            advanceUntilIdle()

            val session = uiState.value.voiceSession
            assertFalse(session.isProcessing)
            assertArrayEquals(shortArrayOf(9, 8, 7, 6), session.processedPcm)
            assertArrayEquals(shortArrayOf(6, 5, 4, 3), session.debugMainVoicePcm)
            assertTrue(session.debugSubvoicePcm.isEmpty())
            assertArrayEquals(shortArrayOf(2, 0, -2, 0), session.debugSignalOverlayPcm)
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
        }

    @Test
    fun stopRecordingProcessesFullRecordedAudio() =
        runTest {
            val recordingGateway =
                FakeVoiceRecordingGateway(
                    stopRecordingResult = shortArrayOf(1, 2, 3, 4),
                    emittedChunks = listOf(shortArrayOf(1, 2), shortArrayOf(3, 4)),
                )
            val gateway =
                FakeVoiceFxGateway(
                    result =
                        VoiceFxProcessResult.Success(
                            finalMix = shortArrayOf(11, 12, 13, 14),
                            mainVoice = shortArrayOf(21, 22, 23, 24),
                            subvoice = shortArrayOf(31, 32, 33, 34),
                            signalOverlay = shortArrayOf(),
                        ),
                )
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession = VoiceSessionState(hasRecordPermission = true),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = gateway,
                    voiceRecordingGateway = recordingGateway,
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartRecording()
            actions.onStopRecording()
            advanceUntilIdle()

            val session = uiState.value.voiceSession
            assertFalse(session.isRecording)
            assertFalse(session.isProcessing)
            assertArrayEquals(shortArrayOf(1, 2, 3, 4), session.inputPcm)
            assertArrayEquals(shortArrayOf(11, 12, 13, 14), session.processedPcm)
            assertArrayEquals(shortArrayOf(21, 22, 23, 24), session.debugMainVoicePcm)
            assertArrayEquals(shortArrayOf(31, 32, 33, 34), session.debugSubvoicePcm)
            assertArrayEquals(shortArrayOf(1, 2, 3, 4), gateway.lastPcm)
            assertEquals(0, gateway.createProcessorCalls)
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
        }

    @Test
    fun recordProcessingModeSelectionClearsProcessedState() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 2, 3),
                                processedPcm = shortArrayOf(4, 5, 6),
                                debugMainVoicePcm = shortArrayOf(7),
                                debugSubvoicePcm = shortArrayOf(8),
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onRecordProcessingModeSelected(VoiceRecordProcessingModeOption.WhileRecording)

            val session = uiState.value.voiceSession
            assertEquals(VoiceRecordProcessingModeOption.WhileRecording, session.selectedRecordProcessingMode)
            assertTrue(session.inputPcm.isNotEmpty())
            assertTrue(session.processedPcm.isEmpty())
            assertTrue(session.debugMainVoicePcm.isEmpty())
            assertTrue(session.debugSubvoicePcm.isEmpty())
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
        }

    @Test
    fun stopRecordingInWhileRecordingModeUsesStreamedProcessingResult() =
        runTest {
            val recordingGateway =
                FakeVoiceRecordingGateway(
                    stopRecordingResult = shortArrayOf(1, 2, 3, 4),
                    emittedChunks = listOf(shortArrayOf(1, 2), shortArrayOf(3, 4)),
                )
            val gateway =
                FakeVoiceFxGateway(
                    result =
                        VoiceFxProcessResult.Success(
                            finalMix = shortArrayOf(99),
                            mainVoice = shortArrayOf(),
                            subvoice = shortArrayOf(),
                            signalOverlay = shortArrayOf(),
                        ),
                    processorSession =
                        FakeVoiceFxProcessorSession(
                            processedBlocks =
                                listOf(
                                    VoiceFxProcessResult.Success(
                                        finalMix = shortArrayOf(11, 12),
                                        mainVoice = shortArrayOf(),
                                        subvoice = shortArrayOf(),
                                        signalOverlay = shortArrayOf(),
                                    ),
                                    VoiceFxProcessResult.Success(
                                        finalMix = shortArrayOf(13, 14),
                                        mainVoice = shortArrayOf(),
                                        subvoice = shortArrayOf(),
                                        signalOverlay = shortArrayOf(),
                                    ),
                                ),
                            flushResult =
                                VoiceFxProcessResult.Success(
                                    finalMix = shortArrayOf(15),
                                    mainVoice = shortArrayOf(),
                                    subvoice = shortArrayOf(),
                                    signalOverlay = shortArrayOf(),
                                ),
                        ),
                )
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedRecordProcessingMode = VoiceRecordProcessingModeOption.WhileRecording,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = gateway,
                    voiceRecordingGateway = recordingGateway,
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartRecording()
            actions.onStopRecording()
            advanceUntilIdle()

            val session = uiState.value.voiceSession
            assertFalse(session.isRecording)
            assertFalse(session.isProcessing)
            assertArrayEquals(shortArrayOf(1, 2, 3, 4), session.inputPcm)
            assertArrayEquals(shortArrayOf(11, 12, 13, 14, 15), session.processedPcm)
            assertTrue(gateway.lastPcm.isEmpty())
            assertEquals(1, gateway.createProcessorCalls)
            assertEquals(BagApiCodes.ERROR_OK, session.lastErrorCode)
        }

    @Test
    fun importVoiceAudioStoresUploadedClip() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                selectedInputSource = VoiceInputSourceOption.Upload,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway =
                        FakeVoiceAudioFileGateway(
                            importResult =
                                VoiceAudioImportResult.Success(
                                    ImportedVoiceAudio(
                                        displayName = "cant.wav",
                                        sampleRateHz = 22050,
                                        pcm = shortArrayOf(5, 4, 3, 2),
                                    ),
                                ),
                        ),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onImportAudio("content://voice/cant.wav")
            advanceUntilIdle()

            val session = uiState.value.voiceSession
            assertFalse(session.isLoadingInput)
            assertArrayEquals(shortArrayOf(5, 4, 3, 2), session.inputPcm)
            assertEquals("cant.wav", session.inputDisplayName)
            assertEquals(22050, session.sampleRateHz)
        }

    @Test
    fun requestVoiceExportCreatesVoiceDocumentRequest() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                selectedInputSource = VoiceInputSourceOption.Upload,
                                inputDisplayName = "vox_input.wav",
                                processedPcm = shortArrayOf(1, 2, 3),
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onRequestExportProcessedAudioToDocument()

            val request = uiState.value.pendingDocumentExportRequest
            assertEquals(AudioDocumentExportSource.Voice, request?.source)
            assertEquals("binharic_vox_input.wav", request?.suggestedFileName)
        }

    @Test
    fun shareProcessedAudioUsesSavedAudioRepository() =
        runTest {
            val repository = FakeSavedAudioRepository()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                processedPcm = shortArrayOf(1, 2, 3, 4),
                                sampleRateHz = 44100,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    savedAudioRepository = repository,
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onShareProcessedAudio()
            advanceUntilIdle()

            assertEquals(0, repository.shareGeneratedAudioCalls)
            assertEquals(1, repository.shareRawPcmAudioCalls)
            assertEquals("binharic_recorded_voice.wav", repository.lastSharedDisplayName)
            assertArrayEquals(shortArrayOf(1, 2, 3, 4), repository.lastSharedPcm)
            assertEquals(44100, repository.lastSharedSampleRateHz)
        }

    @Test
    fun previewTrackSeekStoresSelectedTrackAndPosition() =
        runTest {
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                inputPcm = shortArrayOf(1, 2, 3, 4),
                                processedPcm = shortArrayOf(5, 6, 7, 8, 9),
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = FakeVoiceLiveGateway(),
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onPreviewTrackSeek(VoicePreviewTrackOption.Output, 3)

            val session = uiState.value.voiceSession
            assertEquals(VoicePreviewTrackOption.Output, session.previewTrack)
            assertEquals(3, session.previewPositionSamples)
        }

    @Test
    fun startLiveUpdatesRunningStateAndStopsOnExternalPlayback() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals("Phone mic", uiState.value.voiceSession.liveInputRouteLabel)
            assertEquals("Bluetooth speaker", uiState.value.voiceSession.liveOutputRouteLabel)
            assertTrue(uiState.value.voiceSession.liveSpeakerOutRequested)
            assertTrue(uiState.value.voiceSession.liveSpeakerOutActive)

            actions.stopPreviewForExternalPlayback()

            assertFalse(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.stopCalls)
        }

    @Test
    fun startLiveAcceptsSignalCantPreset() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                                selectedTrackMode = VoiceTrackModeOption.Single,
                                selectedPreset = VoiceFxPresetOption.SignalCant,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals(VoiceFxPresetOption.SignalCant, liveGateway.lastConfig?.preset)
        }

    @Test
    fun startLiveAcceptsBinharicPreset() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                                selectedTrackMode = VoiceTrackModeOption.Dual,
                                selectedPreset = VoiceFxPresetOption.Binharic,
                                selectedSubvoiceStyle = VoiceFxSubvoiceStyleOption.Litany,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals(VoiceFxPresetOption.Binharic, liveGateway.lastConfig?.preset)
            assertEquals(VoiceFxSubvoiceStyleOption.Litany, liveGateway.lastConfig?.subvoiceStyle)
        }

    @Test
    fun startLiveAcceptsRawConstantPreset() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                                selectedTrackMode = VoiceTrackModeOption.Dual,
                                selectedPreset = VoiceFxPresetOption.RawConstant,
                                selectedSubvoiceStyle = VoiceFxSubvoiceStyleOption.Zeal,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals(VoiceFxPresetOption.RawConstant, liveGateway.lastConfig?.preset)
            assertEquals(VoiceFxSubvoiceStyleOption.Zeal, liveGateway.lastConfig?.subvoiceStyle)
        }

    @Test
    fun startLiveAcceptsVoiceTriggerPreset() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                                selectedTrackMode = VoiceTrackModeOption.Dual,
                                selectedPreset = VoiceFxPresetOption.VoiceTrigger,
                                selectedSubvoiceStyle = VoiceFxSubvoiceStyleOption.Void,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals(VoiceFxPresetOption.VoiceTrigger, liveGateway.lastConfig?.preset)
            assertEquals(VoiceFxSubvoiceStyleOption.Void, liveGateway.lastConfig?.subvoiceStyle)
        }

    @Test
    fun startLiveAcceptsRobotVoxPreset() =
        runTest {
            val liveGateway = FakeVoiceLiveGateway()
            val uiState =
                MutableStateFlow(
                    AudioAppUiState(
                        voiceSession =
                            VoiceSessionState(
                                hasRecordPermission = true,
                                selectedWorkflowMode = VoiceWorkflowModeOption.Live,
                                selectedTrackMode = VoiceTrackModeOption.Single,
                                selectedPreset = VoiceFxPresetOption.RobotVox,
                            ),
                    ),
                )
            val actions =
                VoiceSessionActions(
                    uiState = uiState,
                    scope = this,
                    voiceFxGateway = FakeVoiceFxGateway(),
                    voiceRecordingGateway = FakeVoiceRecordingGateway(),
                    voiceLiveGateway = liveGateway,
                    voiceAudioFileGateway = FakeVoiceAudioFileGateway(),
                    voicePreviewPlayer = VoicePreviewPlayer(),
                    stopGlobalPlayback = {},
                    workerDispatcher = StandardTestDispatcher(testScheduler),
                )

            actions.onStartLive()

            assertTrue(uiState.value.voiceSession.isLiveActive)
            assertEquals(1, liveGateway.startCalls)
            assertEquals(VoiceFxPresetOption.RobotVox, liveGateway.lastConfig?.preset)
        }

    private class FakeVoiceFxGateway(
        private val result: VoiceFxProcessResult =
            VoiceFxProcessResult.Failed(BagApiCodes.ERROR_INTERNAL),
        private val processorSession: VoiceFxProcessorSession? = null,
    ) : VoiceFxGateway {
        var lastPcm: ShortArray = shortArrayOf()
        var createProcessorCalls = 0

        override suspend fun applyVoiceFx(
            preset: VoiceFxPresetOption,
            subvoiceStyle: VoiceFxSubvoiceStyleOption,
            pcm: ShortArray,
            sampleRateHz: Int,
        ): VoiceFxProcessResult {
            lastPcm = pcm.copyOf()
            return result
        }

        override fun createProcessor(
            preset: VoiceFxPresetOption,
            subvoiceStyle: VoiceFxSubvoiceStyleOption,
            sampleRateHz: Int,
            enableDiagnostics: Boolean,
        ): VoiceFxProcessorSession? {
            createProcessorCalls += 1
            return processorSession
        }
    }

    private class FakeVoiceRecordingGateway(
        private val stopRecordingResult: ShortArray = shortArrayOf(),
        private val emittedChunks: List<ShortArray> = emptyList(),
    ) : VoiceRecordingGateway {
        override fun startRecording(
            sampleRateHz: Int,
            onPcmChunk: ((ShortArray) -> Unit)?,
        ): Boolean {
            emittedChunks.forEach { chunk -> onPcmChunk?.invoke(chunk) }
            return true
        }

        override fun stopRecording(): ShortArray = stopRecordingResult

        override fun release() = Unit
    }

    private class FakeVoiceFxProcessorSession(
        private val processedBlocks: List<VoiceFxProcessResult>,
        private val flushResult: VoiceFxProcessResult,
    ) : VoiceFxProcessorSession {
        private var blockIndex = 0

        override fun processBlock(pcm: ShortArray): VoiceFxProcessResult {
            val result = processedBlocks.getOrElse(blockIndex) { flushResult }
            blockIndex += 1
            return result
        }

        override fun flush(): VoiceFxProcessResult = flushResult

        override fun release() = Unit
    }

    private class FakeVoiceLiveGateway : VoiceLiveGateway {
        var startCalls = 0
        var stopCalls = 0
        var lastConfig: VoiceLiveConfig? = null
        private var onStopped: ((Int) -> Unit)? = null

        override fun start(
            config: VoiceLiveConfig,
            onRouteChanged: (VoiceLiveRouteSnapshot) -> Unit,
            onStopped: (errorCode: Int) -> Unit,
        ): Boolean {
            startCalls += 1
            lastConfig = config
            onRouteChanged(
                VoiceLiveRouteSnapshot(
                    inputRouteLabel = "Phone mic",
                    outputRouteLabel = "Bluetooth speaker",
                    speakerOutputRequested = true,
                    speakerOutputActive = true,
                ),
            )
            this.onStopped = onStopped
            return true
        }

        override fun stop() {
            stopCalls += 1
            onStopped?.invoke(BagApiCodes.ERROR_OK)
            onStopped = null
        }

        override fun release() = Unit
    }

    private class FakeVoiceAudioFileGateway(
        private val importResult: VoiceAudioImportResult = VoiceAudioImportResult.Failed,
        private val exportResult: Boolean = true,
    ) : VoiceAudioFileGateway {
        override fun importVoiceAudio(uriString: String): VoiceAudioImportResult = importResult

        override fun exportVoiceAudioToDocument(
            pcm: ShortArray,
            sampleRateHz: Int,
            destinationUriString: String,
        ): Boolean = exportResult
    }

    private class FakeSavedAudioRepository : SavedAudioRepository {
        var shareGeneratedAudioCalls = 0
        var shareRawPcmAudioCalls = 0
        var lastSharedDisplayName: String = ""
        var lastSharedPcm: ShortArray = shortArrayOf()
        var lastSharedSampleRateHz: Int = 0

        override fun suggestGeneratedAudioDisplayName(
            inputText: String,
            metadata: GeneratedAudioMetadata,
        ): String = "voice.wav"

        override fun exportGeneratedAudio(
            inputText: String,
            pcm: ShortArray,
            pcmFilePath: String?,
            sampleRateHz: Int,
            metadata: GeneratedAudioMetadata,
        ): AudioExportResult = AudioExportResult.Failed

        override fun exportGeneratedAudioToDocument(
            inputText: String,
            pcm: ShortArray,
            pcmFilePath: String?,
            sampleRateHz: Int,
            metadata: GeneratedAudioMetadata,
            destinationUriString: String,
        ): Boolean = false

        override fun listSavedAudio(): List<SavedAudioItem> = emptyList()

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
            lastSharedPcm = pcm.copyOf()
            lastSharedSampleRateHz = sampleRateHz
            return true
        }

        override fun shareRawPcmAudio(
            displayName: String,
            pcm: ShortArray,
            sampleRateHz: Int,
        ): Boolean {
            shareRawPcmAudioCalls += 1
            lastSharedDisplayName = displayName
            lastSharedPcm = pcm.copyOf()
            lastSharedSampleRateHz = sampleRateHz
            return true
        }

        override fun shareAudio(
            displayName: String,
            uriString: String,
        ): Boolean = false

        override fun readLibraryMetadata(): SavedAudioLibraryMetadata = SavedAudioLibraryMetadata()

        override fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

        override fun renameSavedAudioFolder(
            folderId: String,
            name: String,
        ): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

        override fun deleteSavedAudioFolder(folderId: String): Boolean = false

        override fun assignSavedAudioToFolder(
            itemIds: Collection<String>,
            folderId: String?,
        ): Boolean = false
    }
}
