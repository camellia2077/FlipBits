package com.bag.audioandroid.ui

import android.util.Log
import com.bag.audioandroid.BuildConfig
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.CustomFactionThemeImportParseResult
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.CustomThemeImportMode
import com.bag.audioandroid.ui.model.MorseSpeedOption
import com.bag.audioandroid.ui.model.PlaybackSpeedOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.model.analyzeMorseText
import com.bag.audioandroid.ui.model.findDuplicateImportedThemePresetId
import com.bag.audioandroid.ui.model.parseCustomMaterialThemeImportText
import com.bag.audioandroid.ui.model.toMaterialBatchConfigText
import com.bag.audioandroid.ui.screen.FlashAlignmentPerfTrace
import com.bag.audioandroid.ui.screen.FlashVisualPerfTrace
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.util.measureElapsedMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

internal class AudioDebugScenarioActions(
    private val uiState: StateFlow<AudioAppUiState>,
    private val scope: CoroutineScope,
    private val sampleInputTextProvider: SampleInputTextProvider,
    private val savedAudioRepository: SavedAudioRepository,
    private val generatedAudioCacheGateway: GeneratedAudioCacheGateway,
    private val sessionStateStore: AudioSessionStateStore,
    private val onTransportModeSelected: (TransportModeOption) -> Unit,
    private val onFlashVoicingStyleSelected: (com.bag.audioandroid.ui.model.FlashVoicingStyleOption) -> Unit,
    private val onMorseSpeedSelected: (MorseSpeedOption) -> Unit,
    private val onLanguageSelected: (AppLanguageOption) -> Unit,
    private val onDemoModeEnabledChanged: (Boolean) -> Unit,
    private val onFlashVisualPerfOverlayEnabledChanged: (Boolean) -> Unit,
    private val onInputTextChange: (String) -> Unit,
    private val onEncode: () -> Unit,
    private val onPlaybackSpeedSelected: (Float) -> Unit,
    private val onShellSavedAudioSelected: (String) -> Unit,
    private val onDecode: () -> Unit,
    private val onOpenPlayerDetailSheet: () -> Unit,
    private val onTabSelected: (AppTab) -> Unit,
    private val onThemeStyleSelected: (ThemeStyleOption) -> Unit,
    private val onCustomMaterialThemeSaved: (CustomFactionThemeSettings, String?) -> Unit,
    private val onCustomMaterialThemesImported: (List<CustomFactionThemeSettings>) -> Unit,
) {
    private var pendingSavedAudioDecodeJob: Job? = null

    fun startFlashDebugScenario(scenario: FlashDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (scenario.scenario == FlashDebugScenarioKind.Ui) {
            disableDebugUiOverlaysForLyricsMeasurement(
                logTag = FLASH_AUTOMATION_TAG,
                requestId = scenario.requestId,
                visualPerfOverlayEnabled = scenario.visualPerfOverlayEnabled,
            )
        }
        applyLanguageOverride(
            languageOverride = scenario.languageOverride,
            requestId = scenario.requestId,
            logTag = FLASH_AUTOMATION_TAG,
        )
        val input =
            resolveDebugInput(
                mode = TransportModeOption.Flash,
                text = scenario.text,
                hasTextOverride = scenario.hasTextOverride,
                sampleLength = scenario.sampleLength,
                sampleId = scenario.sampleId,
                languageOverride = scenario.languageOverride,
                requestId = scenario.requestId,
                logTag = FLASH_AUTOMATION_TAG,
            )
        onTransportModeSelected(TransportModeOption.Flash)
        onFlashVoicingStyleSelected(scenario.style)
        onPlaybackSpeedSelected(PlaybackSpeedOption.fromSpeed(scenario.playbackSpeed).speed)
        applyInput(mode = TransportModeOption.Flash, input = input)
        safeLogD(
            FLASH_AUTOMATION_TAG,
            "inputResolved requestId=${scenario.requestId} source=${input.source} " +
                "sampleId=${input.sampleId.orEmpty()} chars=${input.text.length} " +
                "payloadBytes=${input.text.toByteArray(UTF_8).size} style=${scenario.style.id} " +
                "playbackSpeed=${PlaybackSpeedOption.fromSpeed(scenario.playbackSpeed).speed} " +
                "language=${(scenario.languageOverride ?: uiState.value.selectedLanguage).languageTag}",
        )
        if (scenario.encode) {
            onEncode()
        }
    }

    fun startMiniDebugScenario(scenario: MiniDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (scenario.scenario == FlashDebugScenarioKind.Ui) {
            disableDebugUiOverlaysForLyricsMeasurement(
                logTag = MINI_AUTOMATION_TAG,
                requestId = scenario.requestId,
                visualPerfOverlayEnabled = scenario.visualPerfOverlayEnabled,
            )
        }
        applyLanguageOverride(
            languageOverride = scenario.languageOverride,
            requestId = scenario.requestId,
            logTag = MINI_AUTOMATION_TAG,
        )
        val input =
            resolveDebugInput(
                mode = TransportModeOption.Mini,
                text = scenario.text,
                hasTextOverride = scenario.hasTextOverride,
                sampleLength = scenario.sampleLength,
                sampleId = scenario.sampleId,
                languageOverride = scenario.languageOverride,
                requestId = scenario.requestId,
                logTag = MINI_AUTOMATION_TAG,
            )
        onTransportModeSelected(TransportModeOption.Mini)
        onMorseSpeedSelected(scenario.speed)
        onInputTextChange(input.text)
        val morseAnalysis = analyzeMorseText(input.text)
        safeLogD(
            MINI_AUTOMATION_TAG,
            "inputResolved requestId=${scenario.requestId} source=${input.source} " +
                "sampleId=${input.sampleId.orEmpty()} chars=${input.text.length} " +
                "normalizedChars=${morseAnalysis.normalizedText.length} " +
                "whitespaceChars=${input.text.count(Char::isWhitespace)} " +
                "normalizedWhitespaceChars=${morseAnalysis.normalizedText.count(Char::isWhitespace)} " +
                "unsupported=${morseAnalysis.unsupportedCharacters.size} " +
                "language=${(scenario.languageOverride ?: uiState.value.selectedLanguage).languageTag}",
        )
        if (scenario.encode) {
            onEncode()
        }
    }

    fun startEncodeProgressDebugScenario(scenario: EncodeProgressDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        applyLanguageOverride(
            languageOverride = scenario.languageOverride,
            requestId = scenario.requestId,
            logTag = ENCODE_PROGRESS_AUTOMATION_TAG,
        )
        val input = resolveEncodeProgressDebugInput(scenario)
        onTransportModeSelected(scenario.mode)
        if (scenario.mode == TransportModeOption.Mini) {
            onMorseSpeedSelected(scenario.speed)
        }
        applyInput(mode = scenario.mode, input = input)
        val miniAnalysis =
            input
                .takeIf { scenario.mode == TransportModeOption.Mini }
                ?.let { analyzeMorseText(it.text) }
        safeLogD(
            ENCODE_PROGRESS_AUTOMATION_TAG,
            "inputResolved requestId=${scenario.requestId} mode=${scenario.mode.wireName} " +
                "source=${input.source} sampleId=${input.sampleId.orEmpty()} " +
                "chars=${input.text.length} payloadBytes=${input.text.toByteArray(UTF_8).size} " +
                "normalizedChars=${miniAnalysis?.normalizedText?.length ?: -1} " +
                "normalizedPayloadBytes=${miniAnalysis?.normalizedText?.toByteArray(UTF_8)?.size ?: -1} " +
                "whitespaceChars=${input.text.count(Char::isWhitespace)} " +
                "normalizedWhitespaceChars=${miniAnalysis?.normalizedText?.count(Char::isWhitespace) ?: -1} " +
                "repeat=${scenario.repeatCount} speed=${scenario.speed.id} " +
                "language=${(scenario.languageOverride ?: uiState.value.selectedLanguage).languageTag}",
        )
        if (scenario.encode) {
            onEncode()
        }
    }

    private fun disableDebugUiOverlaysForLyricsMeasurement(
        logTag: String,
        requestId: Long,
        visualPerfOverlayEnabled: Boolean? = null,
    ) {
        // Lyrics layout measurement must start from a quiet UI baseline. Demo mode only adds
        // tap-feedback animation, but we still force it off here so future measurement captures
        // do not inherit any debug-only visual noise from device-local settings state.
        onDemoModeEnabledChanged(false)
        val overlayEnabled = visualPerfOverlayEnabled ?: false
        onFlashVisualPerfOverlayEnabledChanged(overlayEnabled)
        FlashAlignmentPerfTrace.reset()
        FlashVisualPerfTrace.reset()
        safeLogD(
            logTag,
            "measurementBaselineReset requestId=$requestId demoMode=false visualPerfOverlay=$overlayEnabled",
        )
    }

    private fun applyLanguageOverride(
        languageOverride: AppLanguageOption?,
        requestId: Long,
        logTag: String,
    ) {
        val targetLanguage = languageOverride ?: return
        onLanguageSelected(targetLanguage)
        safeLogD(
            logTag,
            "languageApplied requestId=$requestId language=${targetLanguage.languageTag}",
        )
    }

    fun startSavedAudioDebugScenario(scenario: SavedAudioDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val (savedAudioItems, listMs) = measureElapsedMs { savedAudioRepository.listSavedAudio() }
        var target = resolveSavedAudioDebugTarget(savedAudioItems, scenario)
        safeLogD(
            SAVED_AUDIO_AUTOMATION_TAG,
            "selectionResolved requestId=${scenario.requestId} listMs=$listMs " +
                "totalSaved=${savedAudioItems.size} targetItemId=${target?.itemId.orEmpty()} " +
                "targetName=${target?.displayName.orEmpty()}",
        )
        if (target == null && scenario.seedDurationMs > 0L) {
            val (seedResult, seedMs) = measureElapsedMs { seedSavedAudioScenarioFile(scenario) }
            safeLogD(
                SAVED_AUDIO_AUTOMATION_TAG,
                "seedResult requestId=${scenario.requestId} elapsedMs=$seedMs result=${seedResult::class.simpleName}",
            )
            val (seededItems, seededListMs) = measureElapsedMs { savedAudioRepository.listSavedAudio() }
            target = resolveSavedAudioDebugTarget(seededItems, scenario)
                ?: seededItems.maxByOrNull { it.savedAtEpochSeconds }
            safeLogD(
                SAVED_AUDIO_AUTOMATION_TAG,
                "selectionResolvedAfterSeed requestId=${scenario.requestId} listMs=$seededListMs " +
                    "totalSaved=${seededItems.size} targetItemId=${target?.itemId.orEmpty()} " +
                    "targetName=${target?.displayName.orEmpty()}",
            )
        }
        if (target == null) {
            safeLogD(
                SAVED_AUDIO_AUTOMATION_TAG,
                "selectionFailed requestId=${scenario.requestId} reason=no_target",
            )
            return
        }
        val (_, selectMs) = measureElapsedMs { onShellSavedAudioSelected(target.itemId) }
        val loadedItemId =
            uiState.value.selectedSavedAudio
                ?.item
                ?.itemId
        val loaded = loadedItemId == target.itemId
        safeLogD(
            SAVED_AUDIO_AUTOMATION_TAG,
            "selectionApplied requestId=${scenario.requestId} itemId=${target.itemId} " +
                "elapsedMs=$selectMs loaded=$loaded currentSource=${uiState.value.currentPlaybackSource.debugSourceId()}",
        )
        if (!loaded) {
            return
        }
        val (_, openDetailMs) = measureElapsedMs { onOpenPlayerDetailSheet() }
        safeLogD(
            SAVED_AUDIO_AUTOMATION_TAG,
            "openDetail requestId=${scenario.requestId} itemId=${target.itemId} " +
                "elapsedMs=$openDetailMs detailVisible=${uiState.value.isExpandedPlayerVisible}",
        )
        if (scenario.decode) {
            requestSavedAudioDecodeWhenHydrated(
                requestId = scenario.requestId,
                itemId = target.itemId,
            )
        }
    }

    private fun requestSavedAudioDecodeWhenHydrated(
        requestId: Long,
        itemId: String,
    ) {
        pendingSavedAudioDecodeJob?.cancel()
        pendingSavedAudioDecodeJob =
            scope.launch {
                val hydrated =
                    withTimeoutOrNull(SAVED_AUDIO_DECODE_WAIT_TIMEOUT_MS) {
                        uiState
                            .filter { state ->
                                val selected = state.selectedSavedAudio
                                selected?.item?.itemId == itemId &&
                                    !selected.isLoadingContent &&
                                    !selected.isDecodingContent
                            }.first()
                    }?.selectedSavedAudio
                if (hydrated == null) {
                    safeLogD(
                        SAVED_AUDIO_AUTOMATION_TAG,
                        "decodeSkipped requestId=$requestId itemId=$itemId reason=hydrate_timeout",
                    )
                    return@launch
                }
                safeLogD(
                    SAVED_AUDIO_AUTOMATION_TAG,
                    "decodeRequested requestId=$requestId itemId=$itemId hydrated=true mode=${hydrated.item.modeWireName}",
                )
                onDecode()
            }
    }

    fun startAppTabDebugScenario(scenario: AppTabDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        applyLanguageOverride(
            languageOverride = scenario.languageOverride,
            requestId = scenario.requestId,
            logTag = TAB_AUTOMATION_TAG,
        )
        onTabSelected(scenario.tab)
        val selectedTab = uiState.value.selectedTab
        safeLogD(
            TAB_AUTOMATION_TAG,
            "tabSelected requestId=${scenario.requestId} requested=${scenario.tab.automationId} " +
                "selected=${selectedTab.automationId}",
        )
        scenario.settingsImportConfirmAction?.let { confirmAction ->
            runSettingsImportProbe(
                requestId = scenario.requestId,
                confirmAction = confirmAction,
                copyScope = scenario.settingsImportCopyScope ?: SettingsImportCopyScope.Current,
            )
        }
    }

    fun startSettingsImportDebugScenario(scenario: SettingsImportDebugScenario) {
        if (!BuildConfig.DEBUG) {
            return
        }
        applyLanguageOverride(
            languageOverride = scenario.languageOverride,
            requestId = scenario.requestId,
            logTag = SETTINGS_IMPORT_AUTOMATION_TAG,
        )
        onTabSelected(AppTab.Config)
        runSettingsImportProbe(
            requestId = scenario.requestId,
            confirmAction = scenario.confirmAction,
            copyScope = scenario.copyScope,
        )
    }

    private fun runSettingsImportProbe(
        requestId: Long,
        confirmAction: SettingsImportConfirmAction,
        copyScope: SettingsImportCopyScope,
    ) {
        onThemeStyleSelected(ThemeStyleOption.Material)
        safeLogD(
            SETTINGS_IMPORT_AUTOMATION_TAG,
            "baselineApplied requestId=$requestId tab=${uiState.value.selectedTab.automationId} " +
                "themeStyle=${uiState.value.selectedThemeStyle.id}",
        )

        val state = uiState.value
        val currentSettings = state.customMaterialThemeSettings
        val exportedSettings =
            when (copyScope) {
                SettingsImportCopyScope.Current -> listOf(currentSettings)
                SettingsImportCopyScope.All -> state.customMaterialThemePresets
            }
        val exportedText = exportedSettings.toMaterialBatchConfigText()
        safeLogD(
            SETTINGS_IMPORT_AUTOMATION_TAG,
            "copyResolved requestId=$requestId scope=${copyScope.id} presetCount=${exportedSettings.size} " +
                "presetId=${currentSettings.presetId} name=${currentSettings.displayName.trim()} " +
                "primary=${currentSettings.primaryHex} chars=${exportedText.length}",
        )

        when (val parsed = parseCustomMaterialThemeImportText(exportedText)) {
            is CustomFactionThemeImportParseResult.Invalid -> {
                safeLogD(
                    SETTINGS_IMPORT_AUTOMATION_TAG,
                    "importInvalid requestId=$requestId reason=${parsed.error::class.simpleName}",
                )
            }

            is CustomFactionThemeImportParseResult.Valid -> {
                val firstImported = parsed.settings.first()
                safeLogD(
                    SETTINGS_IMPORT_AUTOMATION_TAG,
                    "importParsed requestId=$requestId scope=${copyScope.id} blocks=${parsed.settings.size} " +
                        "name=${firstImported.displayName.trim()} primary=${firstImported.primaryHex}",
                )
                if (parsed.settings.size > 1) {
                    val duplicateCount =
                        parsed.settings.count { candidate ->
                            findDuplicateImportedThemePresetId(
                                existing = uiState.value.customMaterialThemePresets,
                                imported = candidate,
                                mode = CustomThemeImportMode.Material,
                            ) != null
                        }
                    safeLogD(
                        SETTINGS_IMPORT_AUTOMATION_TAG,
                        "batchImportPreview requestId=$requestId scope=${copyScope.id} " +
                            "blocks=${parsed.settings.size} duplicateCount=$duplicateCount " +
                            "newCount=${parsed.settings.size - duplicateCount}",
                    )
                    if (confirmAction != SettingsImportConfirmAction.None) {
                        onCustomMaterialThemesImported(parsed.settings)
                        logSettingsImportApplied(
                            requestId = requestId,
                            action = confirmAction,
                            copyScope = copyScope,
                            importedBlockCount = parsed.settings.size,
                        )
                    }
                    return
                }
                val imported = parsed.settings.single()
                val duplicatePresetId =
                    findDuplicateImportedThemePresetId(
                        existing = uiState.value.customMaterialThemePresets,
                        imported = imported,
                        mode = CustomThemeImportMode.Material,
                    )
                safeLogD(
                    SETTINGS_IMPORT_AUTOMATION_TAG,
                    "duplicatePrompt requestId=$requestId shown=${duplicatePresetId != null} " +
                        "duplicatePresetId=${duplicatePresetId.orEmpty()}",
                )
                when (confirmAction) {
                    SettingsImportConfirmAction.None -> Unit
                    SettingsImportConfirmAction.Overwrite -> {
                        onCustomMaterialThemeSaved(imported, duplicatePresetId)
                        logSettingsImportApplied(requestId, confirmAction, copyScope, parsed.settings.size)
                    }
                    SettingsImportConfirmAction.Copy -> {
                        onCustomMaterialThemeSaved(imported, null)
                        logSettingsImportApplied(requestId, confirmAction, copyScope, parsed.settings.size)
                    }
                }
            }
        }
    }

    private fun logSettingsImportApplied(
        requestId: Long,
        action: SettingsImportConfirmAction,
        copyScope: SettingsImportCopyScope,
        importedBlockCount: Int,
    ) {
        val updatedState = uiState.value
        safeLogD(
            SETTINGS_IMPORT_AUTOMATION_TAG,
            "importApplied requestId=$requestId action=${action.id} scope=${copyScope.id} " +
                "blocks=$importedBlockCount " +
                "presetCount=${updatedState.customMaterialThemePresets.size} " +
                "selectedPresetId=${updatedState.customMaterialThemeSettings.presetId}",
        )
    }

    private fun applyInput(
        mode: TransportModeOption,
        input: DebugResolvedInput,
    ) {
        onInputTextChange(input.text)
        if (input.sampleId != null) {
            sessionStateStore.updateSession(mode) {
                it.copy(
                    sampleInputId = input.sampleId,
                    sampleShuffleState = null,
                    appliedSampleEmojiPrefix = null,
                )
            }
        }
    }

    private fun resolveSavedAudioDebugTarget(
        savedAudioItems: List<com.bag.audioandroid.domain.SavedAudioItem>,
        scenario: SavedAudioDebugScenario,
    ) = scenario.itemId?.let { itemId ->
        savedAudioItems.firstOrNull { it.itemId == itemId }
    } ?: scenario.displayName?.let { displayName ->
        savedAudioItems.firstOrNull { it.displayName == displayName }
    } ?: savedAudioItems.maxByOrNull { it.savedAtEpochSeconds }

    private fun seedSavedAudioScenarioFile(scenario: SavedAudioDebugScenario): AudioExportResult {
        val sampleRateHz = 44_100
        val totalSamples = ((scenario.seedDurationMs * sampleRateHz) / 1_000L).coerceAtLeast(1L)
        val cacheWriter = generatedAudioCacheGateway.createPcmCacheWriter(scenario.seedMode.wireName)
        val metadata =
            GeneratedAudioMetadata(
                mode = scenario.seedMode,
                createdAtIsoUtc = Instant.now().toString(),
                durationMs = scenario.seedDurationMs,
                sampleRateHz = sampleRateHz,
                frameSamples = 2_205,
                pcmSampleCount = totalSamples.toInt(),
                payloadByteCount = 0,
                inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                appVersion = "debug-saved-scenario",
                coreVersion = "debug-saved-scenario",
            )
        return try {
            val zeroChunk = ShortArray(44_100)
            var remainingSamples = totalSamples.toInt()
            while (remainingSamples > 0) {
                val chunkSize = minOf(zeroChunk.size, remainingSamples)
                if (chunkSize == zeroChunk.size) {
                    cacheWriter.appendPcm(zeroChunk)
                } else {
                    cacheWriter.appendPcm(ShortArray(chunkSize))
                }
                remainingSamples -= chunkSize
            }
            cacheWriter.finish()
            savedAudioRepository.exportGeneratedAudio(
                inputText = "saved perf ${scenario.seedDurationMs}ms",
                pcm = shortArrayOf(),
                pcmFilePath = cacheWriter.filePath,
                sampleRateHz = sampleRateHz,
                metadata = metadata,
            )
        } catch (_: Exception) {
            cacheWriter.abort()
            AudioExportResult.Failed
        } finally {
            generatedAudioCacheGateway.deleteCachedFile(cacheWriter.filePath)
        }
    }

    private fun resolveEncodeProgressDebugInput(scenario: EncodeProgressDebugScenario): DebugResolvedInput {
        val input =
            resolveDebugInput(
                mode = scenario.mode,
                text = scenario.text,
                hasTextOverride = scenario.hasTextOverride,
                sampleLength = scenario.sampleLength,
                sampleId = scenario.sampleId,
                languageOverride = scenario.languageOverride,
                requestId = scenario.requestId,
                logTag = ENCODE_PROGRESS_AUTOMATION_TAG,
            )
        val repeatedText =
            if (scenario.repeatCount <= 1) {
                input.text
            } else {
                buildString(input.text.length * scenario.repeatCount) {
                    repeat(scenario.repeatCount) {
                        append(input.text)
                    }
                }
            }
        return input.copy(
            text = repeatedText,
            source =
                if (scenario.repeatCount <= 1) {
                    input.source
                } else {
                    "${input.source}:repeat${scenario.repeatCount}"
                },
        )
    }

    private fun resolveDebugInput(
        mode: TransportModeOption,
        text: String,
        hasTextOverride: Boolean,
        sampleLength: SampleInputLengthOption?,
        sampleId: String?,
        languageOverride: AppLanguageOption?,
        requestId: Long,
        logTag: String,
    ): DebugResolvedInput {
        if (hasTextOverride) {
            return DebugResolvedInput(text = text, sampleId = null, source = "text")
        }

        val state = uiState.value
        val resolvedLanguage = languageOverride ?: state.selectedLanguage
        if (sampleId != null) {
            val sample =
                sampleInputTextProvider.sampleById(
                    mode = mode,
                    language = resolvedLanguage,
                    flavor = state.currentSampleFlavor,
                    sampleId = sampleId,
                )
            if (sample != null) {
                return DebugResolvedInput(text = sample.text, sampleId = sample.id, source = "sample:id")
            }
            safeLogD(
                logTag,
                "inputSampleMissing requestId=$requestId mode=${mode.wireName} sampleId=$sampleId " +
                    "flavor=${state.currentSampleFlavor}",
            )
        }

        if (sampleLength != null) {
            val resolvedSampleId =
                sampleInputTextProvider
                    .sampleIds(mode = mode, flavor = state.currentSampleFlavor, length = sampleLength)
                    .firstOrNull()
            val sample =
                resolvedSampleId?.let {
                    sampleInputTextProvider.sampleById(
                        mode = mode,
                        language = resolvedLanguage,
                        flavor = state.currentSampleFlavor,
                        sampleId = it,
                    )
                }
            if (sample != null) {
                return DebugResolvedInput(text = sample.text, sampleId = sample.id, source = "sample:${sampleLength.id}")
            }
            safeLogD(
                logTag,
                "inputSampleMissing requestId=$requestId mode=${mode.wireName} sampleLength=${sampleLength.id} " +
                    "flavor=${state.currentSampleFlavor}",
            )
        }

        return DebugResolvedInput(text = text, sampleId = null, source = "text")
    }
}

private const val FLASH_AUTOMATION_TAG = "FlashAutomation"
private const val MINI_AUTOMATION_TAG = "MiniAutomation"
private const val ENCODE_PROGRESS_AUTOMATION_TAG = "EncodeProgressAutomation"
private const val SAVED_AUDIO_AUTOMATION_TAG = "SavedAudioAutomation"
private const val SAVED_AUDIO_DECODE_WAIT_TIMEOUT_MS = 30_000L
private const val TAB_AUTOMATION_TAG = "TabAutomation"
private const val SETTINGS_IMPORT_AUTOMATION_TAG = "TabAutomation"

private data class DebugResolvedInput(
    val text: String,
    val sampleId: String?,
    val source: String,
)

private fun safeLogD(
    tag: String,
    message: String,
) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Plain JVM unit tests use the Android stub jar, where Log.d is not implemented.
    }
}

private fun com.bag.audioandroid.ui.model.AudioPlaybackSource.debugSourceId(): String =
    when (this) {
        is com.bag.audioandroid.ui.model.AudioPlaybackSource.Generated -> "generated:${mode.wireName}"
        is com.bag.audioandroid.ui.model.AudioPlaybackSource.Saved -> "saved:$itemId"
    }
