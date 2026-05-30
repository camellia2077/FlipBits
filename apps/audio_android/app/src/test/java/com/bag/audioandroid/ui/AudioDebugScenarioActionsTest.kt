package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.domain.AudioExportResult
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter
import com.bag.audioandroid.domain.SavedAudioContent
import com.bag.audioandroid.domain.SavedAudioFolderMutationResult
import com.bag.audioandroid.domain.SavedAudioImportResult
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.SavedAudioLibraryMetadata
import com.bag.audioandroid.domain.SavedAudioRenameResult
import com.bag.audioandroid.domain.SavedAudioRepository
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.CustomBrandThemeSettings
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.theme.customMaterialPalette
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class AudioDebugScenarioActionsTest {
    @Before
    fun clearLogs() {
        ShadowLog.clear()
    }

    @Test
    fun `settings import probe logs duplicate prompt for current material copy`() {
        val existing = customMaterialSettings("keyboard", "Keyboard", "#2D005F")
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedTab = AppTab.Config,
                    selectedThemeStyle = ThemeStyleOption.Material,
                    customMaterialThemePresets = listOf(existing),
                    selectedPalette = customMaterialPalette(existing),
                ),
            )
        val savedCalls = mutableListOf<Pair<CustomBrandThemeSettings, String?>>()
        val importedBatches = mutableListOf<List<CustomBrandThemeSettings>>()
        val actions =
            createActions(
                uiState = state,
                onCustomMaterialThemeSaved = { settings, replacePresetId ->
                    savedCalls += settings to replacePresetId
                },
                onCustomMaterialThemesImported = { importedBatches += it },
            )

        actions.startAppTabDebugScenario(
            AppTabDebugScenario(
                tab = AppTab.Config,
                settingsImportConfirmAction = SettingsImportConfirmAction.None,
                settingsImportCopyScope = SettingsImportCopyScope.Current,
            ),
        )

        val messages = ShadowLog.getLogsForTag("TabAutomation").map { it.msg }
        assertTrue(messages.any { it.contains("copyResolved") && it.contains("scope=current") && it.contains("presetCount=1") })
        assertTrue(messages.any { it.contains("duplicatePrompt") && it.contains("shown=true") })
        assertTrue(savedCalls.isEmpty())
        assertTrue(importedBatches.isEmpty())
    }

    @Test
    fun `settings import probe logs and applies batch material import for all scope`() {
        val keyboard = customMaterialSettings("keyboard", "Keyboard", "#2D005F")
        val paper = customMaterialSettings("paper", "Paper", "#E5E9F0")
        val state =
            MutableStateFlow(
                AudioAppUiState(
                    selectedTab = AppTab.Config,
                    selectedThemeStyle = ThemeStyleOption.Material,
                    customMaterialThemePresets = listOf(keyboard, paper),
                    selectedPalette = customMaterialPalette(keyboard),
                ),
            )
        val importedBatches = mutableListOf<List<CustomBrandThemeSettings>>()
        val actions =
            createActions(
                uiState = state,
                onCustomMaterialThemeSaved = { _, _ -> },
                onCustomMaterialThemesImported = { importedBatches += it },
            )

        actions.startAppTabDebugScenario(
            AppTabDebugScenario(
                tab = AppTab.Config,
                settingsImportConfirmAction = SettingsImportConfirmAction.Copy,
                settingsImportCopyScope = SettingsImportCopyScope.All,
            ),
        )

        val messages = ShadowLog.getLogsForTag("TabAutomation").map { it.msg }
        assertTrue(messages.any { it.contains("copyResolved") && it.contains("scope=all") && it.contains("presetCount=2") })
        assertTrue(messages.any { it.contains("importParsed") && it.contains("scope=all") && it.contains("blocks=2") })
        assertTrue(messages.any { it.contains("batchImportPreview") && it.contains("duplicateCount=2") && it.contains("newCount=0") })
        assertTrue(messages.any { it.contains("importApplied") && it.contains("scope=all") && it.contains("blocks=2") })
        assertEquals(1, importedBatches.size)
        assertEquals(2, importedBatches.single().size)
    }

    private fun createActions(
        uiState: MutableStateFlow<AudioAppUiState>,
        onCustomMaterialThemeSaved: (CustomBrandThemeSettings, String?) -> Unit,
        onCustomMaterialThemesImported: (List<CustomBrandThemeSettings>) -> Unit,
    ) = AudioDebugScenarioActions(
        uiState = uiState,
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        sampleInputTextProvider = DebugScenarioFakeSampleInputTextProvider(),
        savedAudioRepository = DebugScenarioFakeSavedAudioRepository(),
        generatedAudioCacheGateway = DebugScenarioFakeGeneratedAudioCacheGateway(),
        sessionStateStore = AudioSessionStateStore(uiState),
        onTransportModeSelected = {},
        onFlashVoicingStyleSelected = {},
        onMorseSpeedSelected = {},
        onLanguageSelected = {},
        onDemoModeEnabledChanged = {},
        onFlashVisualPerfOverlayEnabledChanged = {},
        onInputTextChange = {},
        onEncode = {},
        onPlaybackSpeedSelected = {},
        onShellSavedAudioSelected = {},
        onDecode = {},
        onOpenPlayerDetailSheet = {},
        onTabSelected = { tab -> uiState.value = uiState.value.copy(selectedTab = tab) },
        onThemeStyleSelected = { style -> uiState.value = uiState.value.copy(selectedThemeStyle = style) },
        onCustomMaterialThemeSaved = onCustomMaterialThemeSaved,
        onCustomMaterialThemesImported = onCustomMaterialThemesImported,
    )

    private fun customMaterialSettings(
        presetId: String,
        displayName: String,
        primaryHex: String,
    ) = CustomBrandThemeSettings(
        presetId = presetId,
        displayName = displayName,
        primaryHex = primaryHex,
        secondaryHex = primaryHex,
        outlineHexOrNull = primaryHex,
    )
}

private class DebugScenarioFakeSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = SampleInput("default", "sample")

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = listOf("default")

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = SampleInput(sampleId, "sample")
}

private class DebugScenarioFakeSavedAudioRepository : SavedAudioRepository {
    override fun suggestGeneratedAudioDisplayName(
        inputText: String,
        metadata: GeneratedAudioMetadata,
    ): String = "debug.wav"

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

    override fun readLibraryMetadata(): SavedAudioLibraryMetadata = SavedAudioLibraryMetadata()

    override fun createSavedAudioFolder(name: String): SavedAudioFolderMutationResult = SavedAudioFolderMutationResult.Failed

    override fun assignSavedAudioToFolder(
        itemIds: Collection<String>,
        folderId: String?,
    ): Boolean = false
}

private class DebugScenarioFakeGeneratedAudioCacheGateway : GeneratedAudioCacheGateway {
    override fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter =
        object : GeneratedAudioPcmCacheWriter {
            override val filePath: String = "debug.pcm"

            override fun appendPcm(pcm: ShortArray) = Unit

            override fun finish() = Unit

            override fun abort() = Unit
        }

    override fun deleteCachedFile(path: String?) = Unit

    override fun pruneCachedFiles(retainedPaths: Set<String>) = Unit
}
