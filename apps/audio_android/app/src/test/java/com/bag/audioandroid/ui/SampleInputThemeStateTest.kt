package com.bag.audioandroid.ui

import com.bag.audioandroid.data.SampleInput
import com.bag.audioandroid.data.SampleInputTextProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.DefaultCustomFactionThemeSettings
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.AudioAppUiState
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.theme.FactionThemes
import com.bag.audioandroid.ui.theme.customFactionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleInputThemeStateTest {
    private val provider = ThemeStateFakeSampleInputTextProvider()
    private val updater = SampleInputSessionUpdater(provider)

    @Test
    fun `faction theme change within same flavor leaves sampled sessions untouched`() {
        val marsRelic = FactionThemes.first { it.id == "mars_relic" }
        val scarletGuard = FactionThemes.first { it.id == "scarlet_guard" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "sacred-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedFactionTheme(scarletGuard, updater)

        assertEquals("scarlet_guard", updated.selectedFactionTheme.id)
        assertEquals("sacred-en-a", updated.sessions.getValue(TransportModeOption.Flash).inputText)
        assertEquals("a", updated.sessions.getValue(TransportModeOption.Flash).sampleInputId)
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertEquals("sacred-en-b", updated.sessions.getValue(TransportModeOption.Ultra).inputText)
        assertEquals("b", updated.sessions.getValue(TransportModeOption.Ultra).sampleInputId)
    }

    @Test
    fun `faction theme change across flavors refreshes sampled sessions only`() {
        val marsRelic = FactionThemes.first { it.id == "mars_relic" }
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "sacred-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedFactionTheme(ancientAlloy, updater)

        assertEquals("ancient_alloy", updated.selectedFactionTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "dynasty-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "dynasty-en")
    }

    @Test
    fun `faction theme flavor refresh preserves selected long sample length`() {
        val marsRelic = FactionThemes.first { it.id == "mars_relic" }
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val lengthAwareUpdater = SampleInputSessionUpdater(LengthAwareThemeStateSampleInputTextProvider())
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-long-a",
                                sampleInputId = "sacred-long-a",
                                sampleShuffleState =
                                    com.bag.audioandroid.ui.state.SampleInputShuffleState(
                                        flavor = SampleFlavor.SacredMachine,
                                        length = SampleInputLengthOption.Long,
                                        shuffledSampleIds = listOf("sacred-long-a", "sacred-long-b"),
                                        nextSampleIndex = 1,
                                        lastPresentedSampleId = "sacred-long-a",
                                    ),
                            ),
                        TransportModeOption.Pro to ModeAudioSessionState(inputText = "CUSTOM INPUT", sampleInputId = null),
                        TransportModeOption.Ultra to ModeAudioSessionState(inputText = "", sampleInputId = null),
                        TransportModeOption.Mini to ModeAudioSessionState(inputText = "", sampleInputId = null),
                    ),
            )

        val updated = state.withSelectedFactionTheme(ancientAlloy, lengthAwareUpdater)

        val flashSession = updated.sessions.getValue(TransportModeOption.Flash)
        assertTrue(flashSession.inputText.startsWith("dynasty-long-"))
        assertTrue(flashSession.sampleInputId in setOf("dynasty-long-a", "dynasty-long-b"))
        assertEquals(SampleInputLengthOption.Long, flashSession.sampleShuffleState?.length)
    }

    @Test
    fun `faction theme flavor refresh keeps long selection state and long text in sync`() {
        val marsRelic = FactionThemes.first { it.id == "mars_relic" }
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val lengthAwareUpdater = SampleInputSessionUpdater(LengthAwareThemeStateSampleInputTextProvider())
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = marsRelic,
                selectedSampleInputLength = SampleInputLengthOption.Long,
                transportMode = TransportModeOption.Flash,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-long-a",
                                sampleInputId = "sacred-long-a",
                                sampleShuffleState =
                                    com.bag.audioandroid.ui.state.SampleInputShuffleState(
                                        flavor = SampleFlavor.SacredMachine,
                                        length = SampleInputLengthOption.Long,
                                        shuffledSampleIds = listOf("sacred-long-a", "sacred-long-b"),
                                        nextSampleIndex = 1,
                                        lastPresentedSampleId = "sacred-long-a",
                                    ),
                            ),
                        TransportModeOption.Pro to ModeAudioSessionState(),
                        TransportModeOption.Ultra to ModeAudioSessionState(),
                        TransportModeOption.Mini to ModeAudioSessionState(),
                    ),
            )

        val updated = state.withSelectedFactionTheme(ancientAlloy, lengthAwareUpdater)

        assertEquals(SampleInputLengthOption.Long, updated.selectedSampleInputLength)
        assertEquals(TransportModeOption.Flash, updated.transportMode)
        assertEquals(SampleInputLengthOption.Long, updated.currentSession.sampleShuffleState?.length)
        assertTrue(updated.currentSession.inputText.startsWith("dynasty-long-"))
        assertTrue(updated.currentSession.sampleInputId in setOf("dynasty-long-a", "dynasty-long-b"))
    }

    @Test
    fun `switching away from faction theme falls back to sacred machine flavor`() {
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = ancientAlloy,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedThemeStyle(ThemeStyleOption.Material, updater)

        assertEquals(ThemeStyleOption.Material, updated.selectedThemeStyle)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching from sacred machine preset to custom theme keeps sacred machine samples`() {
        val marsRelic = FactionThemes.first { it.id == "mars_relic" }
        val customTheme = customFactionTheme(DefaultCustomFactionThemeSettings)
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = marsRelic,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "sacred-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "sacred-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedFactionTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedFactionTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching from ancient dynasty to custom theme falls back to sacred machine samples`() {
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val customTheme = customFactionTheme(DefaultCustomFactionThemeSettings)
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = ancientAlloy,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedFactionTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedFactionTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    @Test
    fun `switching to named custom preset still uses sacred machine samples`() {
        val ancientAlloy = FactionThemes.first { it.id == "ancient_alloy" }
        val customTheme =
            customFactionTheme(
                DefaultCustomFactionThemeSettings.copy(
                    presetId = "named-custom",
                    displayName = "Named Custom",
                ),
            )
        val state =
            AudioAppUiState(
                selectedLanguage = AppLanguageOption.English,
                selectedThemeStyle = ThemeStyleOption.FactionTheme,
                selectedFactionTheme = ancientAlloy,
                sessions =
                    mapOf(
                        TransportModeOption.Flash to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-a",
                                sampleInputId = "a",
                            ),
                        TransportModeOption.Pro to
                            ModeAudioSessionState(
                                inputText = "CUSTOM INPUT",
                                sampleInputId = null,
                            ),
                        TransportModeOption.Ultra to
                            ModeAudioSessionState(
                                inputText = "dynasty-en-b",
                                sampleInputId = "b",
                            ),
                    ),
            )

        val updated = state.withSelectedFactionTheme(customTheme, updater)

        assertEquals(customTheme.id, updated.selectedFactionTheme.id)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Flash), "sacred-en")
        assertEquals("CUSTOM INPUT", updated.sessions.getValue(TransportModeOption.Pro).inputText)
        assertNull(updated.sessions.getValue(TransportModeOption.Pro).sampleInputId)
        assertFlavorSample(updated.sessions.getValue(TransportModeOption.Ultra), "sacred-en")
    }

    private fun assertFlavorSample(
        session: ModeAudioSessionState,
        expectedPrefix: String,
    ) {
        assertTrue(session.inputText == "$expectedPrefix-a" || session.inputText == "$expectedPrefix-b")
        assertEquals(session.inputText.removePrefix("$expectedPrefix-"), session.sampleInputId)
    }
}

private class LengthAwareThemeStateSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = entries(flavor, SampleInputLengthOption.Short).first()

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = entries(flavor, length).map(SampleInput::id)

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? =
        (entries(flavor, SampleInputLengthOption.Short) + entries(flavor, SampleInputLengthOption.Long))
            .firstOrNull { it.id == sampleId }

    private fun entries(
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<SampleInput> {
        val prefix =
            when (flavor) {
                SampleFlavor.SacredMachine -> "sacred"
                SampleFlavor.AncientDynasty -> "dynasty"
                SampleFlavor.ImmortalRot -> "rot"
                SampleFlavor.ScarletCarnage -> "scarlet"
                SampleFlavor.ExquisiteFall -> "exquisite"
                SampleFlavor.LabyrinthOfMutability -> "labyrinth"
            }
        val lengthPart =
            when (length) {
                SampleInputLengthOption.Short -> "short"
                SampleInputLengthOption.Long -> "long"
            }
        return listOf(
            SampleInput("$prefix-$lengthPart-a", "$prefix-$lengthPart-a"),
            SampleInput("$prefix-$lengthPart-b", "$prefix-$lengthPart-b"),
        )
    }
}

private class ThemeStateFakeSampleInputTextProvider : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput = entries(mode, language, flavor).first()

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = entries(mode, AppLanguageOption.English, flavor).map(SampleInput::id)

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? = entries(mode, language, flavor).firstOrNull { it.id == sampleId }

    private fun entries(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): List<SampleInput> =
        when (mode) {
            TransportModeOption.Pro, TransportModeOption.Mini ->
                listOf(
                    SampleInput("a", "PRO-CUSTOM-A"),
                    SampleInput("b", "PRO-CUSTOM-B"),
                )

            TransportModeOption.Flash, TransportModeOption.Ultra ->
                when (flavor) {
                    SampleFlavor.SacredMachine ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "sacred-ja-a"),
                                    SampleInput("b", "sacred-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "sacred-en-a"),
                                    SampleInput("b", "sacred-en-b"),
                                )
                        }
                    SampleFlavor.AncientDynasty ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "dynasty-ja-a"),
                                    SampleInput("b", "dynasty-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "dynasty-en-a"),
                                    SampleInput("b", "dynasty-en-b"),
                                )
                        }
                    SampleFlavor.ImmortalRot ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "rot-ja-a"),
                                    SampleInput("b", "rot-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "rot-en-a"),
                                    SampleInput("b", "rot-en-b"),
                                )
                        }
                    SampleFlavor.ScarletCarnage ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "scarlet-ja-a"),
                                    SampleInput("b", "scarlet-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "scarlet-en-a"),
                                    SampleInput("b", "scarlet-en-b"),
                                )
                        }
                    SampleFlavor.ExquisiteFall ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "exquisite-ja-a"),
                                    SampleInput("b", "exquisite-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "exquisite-en-a"),
                                    SampleInput("b", "exquisite-en-b"),
                                )
                        }
                    SampleFlavor.LabyrinthOfMutability ->
                        when (language) {
                            AppLanguageOption.Japanese ->
                                listOf(
                                    SampleInput("a", "labyrinth-ja-a"),
                                    SampleInput("b", "labyrinth-ja-b"),
                                )
                            else ->
                                listOf(
                                    SampleInput("a", "labyrinth-en-a"),
                                    SampleInput("b", "labyrinth-en-b"),
                                )
                        }
                }
        }
}
