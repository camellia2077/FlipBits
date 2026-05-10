package com.bag.audioandroid.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.SavedAudioModeFilter
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.currentBitsText
import com.bag.audioandroid.ui.screen.flashBitReadoutFrame
import com.bag.audioandroid.ui.screen.previousBitsText
import com.bag.audioandroid.ui.state.AudioAppUiState
import kotlinx.coroutines.delay

@Composable
fun AudioAndroidApp(
    debugScenario: FlashDebugScenario? = null,
    miniDebugScenario: MiniDebugScenario? = null,
) {
    val appContext = LocalContext.current.applicationContext
    val factory = rememberAudioAndroidViewModelFactory(appContext)
    val viewModel: AudioAndroidViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importAudioLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let { viewModel.onImportAudio(it.toString()) }
        }
    val exportAudioLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("audio/wav"),
        ) { uri ->
            viewModel.onDocumentExportPicked(uri?.toString())
        }
    var savedAudioFilter by rememberSaveable {
        mutableStateOf(defaultSavedAudioFilter(uiState))
    }
    var handledDebugScenarioPlaybackRequestId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var debugScenarioStartRevision by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var miniDebugScenarioStartRevision by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    val headlessSampleRate = uiState.currentPlayback.sampleRateHz.coerceAtLeast(1)
    val headlessLogSampleBucket =
        uiState.currentPlayback.displayedSamples / (headlessSampleRate / 2).coerceAtLeast(1)

    LaunchedEffect(uiState.showSavedAudioSheet, uiState.currentPlaybackSource, uiState.transportMode) {
        if (uiState.showSavedAudioSheet) {
            savedAudioFilter = defaultSavedAudioFilter(uiState)
        }
    }

    LaunchedEffect(uiState.showPlayerDetailSheet, uiState.miniPlayerModel) {
        if (uiState.showPlayerDetailSheet && uiState.miniPlayerModel == null) {
            viewModel.onClosePlayerDetailSheet()
        }
    }

    LaunchedEffect(uiState.pendingDocumentExportRequest?.id) {
        uiState.pendingDocumentExportRequest?.let { request ->
            exportAudioLauncher.launch(request.suggestedFileName)
        }
    }

    LaunchedEffect(debugScenario?.requestId) {
        if (BuildConfig.DEBUG && debugScenario != null) {
            debugScenarioStartRevision =
                uiState.sessions[TransportModeOption.Flash]?.generatedContentRevision ?: 0L
            handledDebugScenarioPlaybackRequestId = null
            viewModel.startFlashDebugScenario(debugScenario)
        }
    }

    LaunchedEffect(miniDebugScenario?.requestId) {
        if (BuildConfig.DEBUG && miniDebugScenario != null) {
            miniDebugScenarioStartRevision =
                uiState.sessions[TransportModeOption.Mini]?.generatedContentRevision ?: 0L
            handledDebugScenarioPlaybackRequestId = null
            viewModel.startMiniDebugScenario(miniDebugScenario)
        }
    }

    LaunchedEffect(
        debugScenario,
        miniDebugScenario,
        uiState.currentSession.isCodecBusy,
        uiState.currentSession.generatedContentRevision,
        uiState.miniPlayerModel,
    ) {
        if (shouldStartUiDebugPlayback(debugScenario, debugScenarioStartRevision, handledDebugScenarioPlaybackRequestId, uiState)) {
            val scenario = debugScenario ?: return@LaunchedEffect
            handledDebugScenarioPlaybackRequestId = scenario.requestId
            viewModel.onOpenPlayerDetailSheet()
            viewModel.onTogglePlayback()
            if (scenario.playDurationMs > 0L) {
                delay(scenario.playDurationMs)
                viewModel.stopPlayback()
            }
        }
        if (shouldStartUiDebugPlayback(miniDebugScenario, miniDebugScenarioStartRevision, handledDebugScenarioPlaybackRequestId, uiState)) {
            val scenario = miniDebugScenario ?: return@LaunchedEffect
            handledDebugScenarioPlaybackRequestId = scenario.requestId
            viewModel.onOpenPlayerDetailSheet()
            viewModel.onTogglePlayback()
            if (scenario.playDurationMs > 0L) {
                delay(scenario.playDurationMs)
                viewModel.stopPlayback()
            }
        }
    }

    LaunchedEffect(
        debugScenario,
        uiState.currentSession.isCodecBusy,
        uiState.currentSession.generatedContentRevision,
        uiState.currentSession.followData.followAvailable,
        uiState.currentSession.followData.binaryGroupTimeline.size,
    ) {
        if (shouldStartFlashHeadlessDebugPlayback(
                debugScenario,
                debugScenarioStartRevision,
                handledDebugScenarioPlaybackRequestId,
                uiState,
            )
        ) {
            val scenario = debugScenario ?: return@LaunchedEffect
            val followData = uiState.currentSession.followData
            handledDebugScenarioPlaybackRequestId = scenario.requestId
            Log.d(
                FlashHeadlessTag,
                "start requestId=${scenario.requestId} style=${scenario.style.id} " +
                    "revision=${uiState.currentSession.generatedContentRevision} totalSamples=${uiState.currentPlaybackSampleCount} " +
                    "follow=${followData.followAvailable} binaryGroups=${followData.binaryGroupTimeline.size}",
            )
            viewModel.onTogglePlayback()
            if (scenario.playDurationMs > 0L) {
                delay(scenario.playDurationMs)
                viewModel.stopPlayback()
                Log.d(FlashHeadlessTag, "stop requestId=${scenario.requestId} playMs=${scenario.playDurationMs}")
            }
        }
    }

    LaunchedEffect(
        debugScenario?.requestId,
        headlessLogSampleBucket,
        uiState.currentPlayback.isPlaying,
        uiState.currentPlaybackFollowData,
    ) {
        if (
            BuildConfig.DEBUG &&
            debugScenario?.scenario == FlashDebugScenarioKind.Headless &&
            handledDebugScenarioPlaybackRequestId == debugScenario.requestId
        ) {
            val followData = uiState.currentPlaybackFollowData
            val sample = uiState.currentPlayback.displayedSamples
            val frame = flashBitReadoutFrame(followData, sample.toFloat())
            Log.d(
                FlashHeadlessTag,
                "tick requestId=${debugScenario.requestId} playing=${uiState.currentPlayback.isPlaying} " +
                    "sample=$sample total=${uiState.currentPlaybackSampleCount} " +
                    "follow=${followData.followAvailable} textFollow=${followData.textFollowAvailable} " +
                    "binaryGroups=${followData.binaryGroupTimeline.size} bit=${frame?.currentBitOffset ?: -1} " +
                    "revealed=${frame?.revealedBitOffset ?: -1} group=${frame?.currentGroupStartIndex ?: -1} " +
                    "prev=${frame?.previousBitsText().orEmpty()} current=${frame?.currentBitsText().orEmpty()}",
            )
        }
    }

    AudioAndroidAppShell(
        uiState = uiState,
        savedAudioFilter = savedAudioFilter,
        onSavedAudioFilterChange = { savedAudioFilter = it },
        debugScenario = debugScenario,
        onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
        viewModel = viewModel,
    )
}

private const val FlashHeadlessTag = "FlashHeadless"

private fun shouldStartUiDebugPlayback(
    scenario: FlashDebugScenario?,
    startRevision: Long?,
    handledRequestId: Long?,
    uiState: AudioAppUiState,
): Boolean {
    if (!BuildConfig.DEBUG || scenario == null) {
        return false
    }
    val scenarioReady = scenario.play && scenario.scenario == FlashDebugScenarioKind.Ui
    val requestPending = handledRequestId != scenario.requestId
    val appReady = isGeneratedPlaybackReady(TransportModeOption.Flash, startRevision, uiState)
    return scenarioReady && requestPending && appReady
}

private fun shouldStartUiDebugPlayback(
    scenario: MiniDebugScenario?,
    startRevision: Long?,
    handledRequestId: Long?,
    uiState: AudioAppUiState,
): Boolean {
    if (!BuildConfig.DEBUG || scenario == null) {
        return false
    }
    val scenarioReady = scenario.play && scenario.scenario == FlashDebugScenarioKind.Ui
    val requestPending = handledRequestId != scenario.requestId
    val appReady = isGeneratedPlaybackReady(TransportModeOption.Mini, startRevision, uiState)
    return scenarioReady && requestPending && appReady
}

private fun isGeneratedPlaybackReady(
    targetMode: TransportModeOption,
    startRevision: Long?,
    uiState: AudioAppUiState,
): Boolean {
    val generationReady = uiState.currentSession.generatedContentRevision > (startRevision ?: -1L)
    val playbackReady = uiState.miniPlayerModel != null && !uiState.currentPlayback.isPlaying
    val sessionReady = uiState.transportMode == targetMode && !uiState.currentSession.isCodecBusy
    return generationReady && playbackReady && sessionReady
}

private fun shouldStartFlashHeadlessDebugPlayback(
    scenario: FlashDebugScenario?,
    startRevision: Long?,
    handledRequestId: Long?,
    uiState: AudioAppUiState,
): Boolean {
    if (!BuildConfig.DEBUG || scenario == null) {
        return false
    }
    val scenarioReady = scenario.play && scenario.scenario == FlashDebugScenarioKind.Headless
    val requestPending = handledRequestId != scenario.requestId
    val appReady = isFlashHeadlessPlaybackReady(startRevision, uiState)
    return scenarioReady && requestPending && appReady
}

private fun isFlashHeadlessPlaybackReady(
    startRevision: Long?,
    uiState: AudioAppUiState,
): Boolean {
    val generationReady = uiState.currentSession.generatedContentRevision > (startRevision ?: -1L)
    val followReady =
        uiState.currentSession.followData.followAvailable &&
            uiState.currentSession.followData.binaryGroupTimeline
                .isNotEmpty()
    val sessionReady = uiState.transportMode == TransportModeOption.Flash && !uiState.currentSession.isCodecBusy
    return generationReady && followReady && sessionReady
}

private fun defaultSavedAudioFilter(uiState: AudioAppUiState): SavedAudioModeFilter =
    when (val source = uiState.currentPlaybackSource) {
        is AudioPlaybackSource.Generated -> SavedAudioModeFilter.fromTransportMode(source.mode)
        is AudioPlaybackSource.Saved ->
            SavedAudioModeFilter.entries.firstOrNull {
                it.mode?.wireName == uiState.currentSavedAudioItem?.modeWireName
            } ?: SavedAudioModeFilter.All
    }
