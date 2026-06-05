package com.bag.audioandroid.ui.state

import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioInputSourceKind
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.WavAudioInfo
import com.bag.audioandroid.ui.model.AudioPlaybackSource
import com.bag.audioandroid.ui.model.CustomFactionThemeSettings
import com.bag.audioandroid.ui.model.ThemeModeOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.screen.LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD
import com.bag.audioandroid.ui.theme.customMaterialPalette
import com.bag.audioandroid.ui.theme.customMaterialPaletteId
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioAppUiStateTest {
    @Test
    fun `generated playback frame samples fall back to default when metadata is missing`() {
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Pro),
            )

        assertEquals(2205, state.currentPlaybackFrameSamples)
    }

    @Test
    fun `long generated flash visual data prefers waveform preview`() {
        val fullPcm = ShortArray(16) { it.toShort() }
        val previewPcm = shortArrayOf(1, 2, 3)
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Generated(TransportModeOption.Flash),
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                generatedPcm = fullPcm,
                                generatedWaveformPcm = previewPcm,
                                generatedAudioMetadata =
                                    GeneratedAudioMetadata(
                                        mode = TransportModeOption.Flash,
                                        createdAtIsoUtc = "2026-01-01T00:00:00Z",
                                        durationMs = 120_000L,
                                        sampleRateHz = 44_100,
                                        frameSamples = 2205,
                                        pcmSampleCount = LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD,
                                        payloadByteCount = 1_656,
                                        inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                                        appVersion = "1.0.0",
                                        coreVersion = "1.0.0",
                                    ),
                            ),
                        TransportModeOption.Pro to ModeAudioSessionState(),
                        TransportModeOption.Ultra to ModeAudioSessionState(),
                        TransportModeOption.Mini to ModeAudioSessionState(),
                    ),
            )

        val visualData = state.currentPlaybackVisualData

        assertEquals(PlaybackPcmVisualKind.WaveformPreview, visualData.kind)
        assertEquals(previewPcm.toList(), visualData.samples.toList())
        assertEquals(LONG_AUDIO_VISUALIZATION_SAMPLE_THRESHOLD, visualData.totalSamples)
    }

    @Test
    fun `saved playback frame samples prefer metadata value`() {
        val savedItem =
            SavedAudioItem(
                itemId = "saved-1",
                displayName = "Saved Pro",
                uriString = "content://saved/pro",
                modeWireName = TransportModeOption.Pro.wireName,
                durationMs = 1_000L,
                savedAtEpochSeconds = 100L,
            )
        val metadata =
            GeneratedAudioMetadata(
                mode = TransportModeOption.Pro,
                createdAtIsoUtc = "2026-01-01T00:00:00Z",
                durationMs = 1_000L,
                sampleRateHz = 44_100,
                frameSamples = 4096,
                pcmSampleCount = 8_192,
                payloadByteCount = 12,
                inputSourceKind = GeneratedAudioInputSourceKind.Manual,
                appVersion = "1.0.0",
                coreVersion = "1.0.0",
            )
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Saved(savedItem.itemId),
                selectedSavedAudio =
                    SavedAudioPlaybackSelection(
                        item = savedItem,
                        pcm = shortArrayOf(1, 2, 3),
                        sampleRateHz = 44_100,
                        metadata = metadata,
                        playback = PlaybackUiState(),
                    ),
            )

        assertEquals(4096, state.currentPlaybackFrameSamples)
    }

    @Test
    fun `saved playback flash signal info comes from selection cache`() {
        val savedItem =
            SavedAudioItem(
                itemId = "saved-flash",
                displayName = "Saved Flash",
                uriString = "content://saved/flash",
                modeWireName = TransportModeOption.Flash.wireName,
                durationMs = 1_000L,
                savedAtEpochSeconds = 100L,
            )
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Saved(savedItem.itemId),
                selectedSavedAudio =
                    SavedAudioPlaybackSelection(
                        item = savedItem,
                        pcm = shortArrayOf(1, 2, 3),
                        sampleRateHz = 44_100,
                        playback = PlaybackUiState(),
                        flashSignalInfo =
                            FlashSignalInfo(
                                lowCarrierHz = "300",
                                highCarrierHz = "600",
                                bitDurationSamples = "2205",
                                payloadSilence = "none",
                                decodePath = "fixed low/high window",
                                available = true,
                            ),
                    ),
            )

        assertEquals("300", state.currentPlaybackFlashSignalInfo.lowCarrierHz)
        assertEquals("600", state.currentPlaybackFlashSignalInfo.highCarrierHz)
    }

    @Test
    fun `saved playback wav audio info comes from selection cache`() {
        val savedItem =
            SavedAudioItem(
                itemId = "saved-info",
                displayName = "Saved Info",
                uriString = "content://saved/info",
                modeWireName = TransportModeOption.Flash.wireName,
                durationMs = 1_000L,
                savedAtEpochSeconds = 100L,
            )
        val state =
            AudioAppUiState(
                currentPlaybackSource = AudioPlaybackSource.Saved(savedItem.itemId),
                selectedSavedAudio =
                    SavedAudioPlaybackSelection(
                        item = savedItem,
                        pcm = shortArrayOf(1, 2, 3),
                        sampleRateHz = 44_100,
                        playback = PlaybackUiState(),
                        wavAudioInfo =
                            WavAudioInfo(
                                wavStatusCode = com.bag.audioandroid.domain.AudioIoWavCodes.STATUS_OK,
                                sampleRateHz = 48_000,
                                channels = 1,
                                bitsPerSample = 16,
                                pcmSampleCount = 96_000L,
                                dataByteCount = 192_000L,
                                fileByteCount = 192_044L,
                                durationMs = 2_000L,
                            ),
                    ),
            )

        assertEquals(48_000, state.currentPlaybackWavAudioInfo.sampleRateHz)
        assertEquals(192_044L, state.currentPlaybackWavAudioInfo.fileByteCount)
    }

    @Test
    fun `material palette id for light prefers light selection`() {
        val state =
            AudioAppUiState(
                selectedMaterialPaletteIdLight = customMaterialPaletteId("light"),
                selectedMaterialPaletteIdDark = customMaterialPaletteId("dark"),
            )

        assertEquals(customMaterialPaletteId("light"), state.materialPaletteIdForMode(isDarkTheme = false))
    }

    @Test
    fun `material palette id for dark prefers dark selection`() {
        val state =
            AudioAppUiState(
                selectedMaterialPaletteIdLight = customMaterialPaletteId("light"),
                selectedMaterialPaletteIdDark = customMaterialPaletteId("dark"),
            )

        assertEquals(customMaterialPaletteId("dark"), state.materialPaletteIdForMode(isDarkTheme = true))
    }

    @Test
    fun `switching to dark theme mode restores last dark custom palette`() {
        val state =
            AudioAppUiState(
                customMaterialThemePresets =
                    listOf(
                        customMaterialSettings("light", "#D0BCFF"),
                        customMaterialSettings("dark", "#7D5260"),
                    ),
                selectedMaterialPaletteIdLight = customMaterialPaletteId("light"),
                selectedMaterialPaletteIdDark = customMaterialPaletteId("dark"),
            )

        val updated = state.withSelectedThemeMode(ThemeModeOption.Dark)

        assertEquals(ThemeModeOption.Dark, updated.selectedThemeMode)
        assertEquals(true, updated.isMaterialDarkThemeActive)
        assertEquals(customMaterialPaletteId("dark"), updated.selectedPalette.id)
    }

    @Test
    fun `switching back to light restores last light custom palette`() {
        val darkSettings = customMaterialSettings("dark", "#7D5260")
        val state =
            AudioAppUiState(
                customMaterialThemePresets =
                    listOf(
                        customMaterialSettings("light", "#D0BCFF"),
                        darkSettings,
                    ),
                selectedThemeMode = ThemeModeOption.Dark,
                isMaterialDarkThemeActive = true,
                selectedPalette = customMaterialPalette(darkSettings),
                selectedMaterialPaletteIdLight = customMaterialPaletteId("light"),
                selectedMaterialPaletteIdDark = customMaterialPaletteId("dark"),
            )

        val updated = state.withSelectedThemeMode(ThemeModeOption.Light)

        assertEquals(ThemeModeOption.Light, updated.selectedThemeMode)
        assertEquals(false, updated.isMaterialDarkThemeActive)
        assertEquals(customMaterialPaletteId("light"), updated.selectedPalette.id)
    }

    private fun customMaterialSettings(
        presetId: String,
        primaryHex: String,
    ) = CustomFactionThemeSettings(
        presetId = presetId,
        displayName = presetId,
        primaryHex = primaryHex,
        secondaryHex = primaryHex,
        outlineHexOrNull = primaryHex,
    )
}
