package com.bag.audioandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidSampleInputTextProviderTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `localized thematic samples follow app language while pro stays ascii for both flavors`() {
        val provider = AndroidSampleInputTextProvider(context)
        val sacredChinese =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.Chinese,
                SampleFlavor.SacredMachine,
            )
        val sacredTraditionalChinese =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.TraditionalChinese,
                SampleFlavor.SacredMachine,
            )
        val sacredEnglish =
            provider.defaultSample(
                TransportModeOption.Ultra,
                AppLanguageOption.English,
                SampleFlavor.SacredMachine,
            )
        val sacredJapanese =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.Japanese,
                SampleFlavor.SacredMachine,
            )

        assertEquals("caliper_oil_rite", sacredChinese.id)
        assertEquals("caliper_oil_rite", sacredTraditionalChinese.id)
        assertEquals("caliper_oil_rite", sacredEnglish.id)
        assertEquals("caliper_oil_rite", sacredJapanese.id)
        assertTrue(sacredChinese.text.isNotBlank())
        assertTrue(sacredTraditionalChinese.text.isNotBlank())
        assertTrue(sacredEnglish.text.isNotBlank())
        assertTrue(sacredJapanese.text.isNotBlank())
        assertTrue(sacredEnglish.text.all { it.code in 0..0x7F })
        assertTrue(sacredChinese.text != sacredEnglish.text)
        assertTrue(sacredTraditionalChinese.text != sacredEnglish.text)
        assertTrue(sacredJapanese.text != sacredEnglish.text)

        AppLanguageOption.entries.forEach { language ->
            val sacredProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.SacredMachine,
                ).text
            assertEquals("APPLY SACRED OIL TO BRASS CALIPERS", sacredProText)
            assertTrue(sacredProText.all { it.code in 0..0x7F })

            val dynastyProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.AncientDynasty,
                ).text
            assertEquals("IMMORTAL ALLOY HAND CLOSES. NO WARMTH ANSWERS.", dynastyProText)
            assertTrue(dynastyProText.all { it.code in 0..0x7F })

            val rotProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ImmortalRot,
                ).text
            assertEquals("BRIGHT MUSHROOMS RISE FROM EMPTY EYES", rotProText)
            assertTrue(rotProText.all { it.code in 0..0x7F })

            val scarletProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ScarletCarnage,
                ).text
            assertEquals("EYES BURST RED. BLOOD BOILS. TEETH KEEP BITING.", scarletProText)
            assertTrue(scarletProText.all { it.code in 0..0x7F })

            val exquisiteProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.ExquisiteFall,
                ).text
            assertEquals("GOLD DUST FILLS THE LUNGS. HANDS STILL REACH FOR MORE.", exquisiteProText)
            assertTrue(exquisiteProText.all { it.code in 0..0x7F })

            val labyrinthProText =
                provider.defaultSample(
                    TransportModeOption.Pro,
                    language,
                    SampleFlavor.LabyrinthOfMutability,
                ).text
            assertEquals("HERO CUTS ONE STRING. THE NEXT NOOSE TIGHTENS.", labyrinthProText)
            assertTrue(labyrinthProText.all { it.code in 0..0x7F })
        }
    }

    @Test
    fun `default sample keeps the same semantic slot while flavor changes the text`() {
        val provider = AndroidSampleInputTextProvider(context)

        val sacred =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.SacredMachine,
            )
        val dynasty =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.AncientDynasty,
            )

        // The sample prose is creative content and changes often; lock the semantic slot id,
        // then only verify that Android resources resolve to non-empty text.
        assertEquals("caliper_oil_rite", sacred.id)
        assertTrue(sacred.text.isNotBlank())
        val rot =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ImmortalRot,
            )

        assertEquals("alloy_hand_no_warmth", dynasty.id)
        assertTrue(dynasty.text.isNotBlank())
        val scarlet =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ScarletCarnage,
            )

        assertEquals("mushrooms_from_empty_eyes", rot.id)
        assertTrue(rot.text.isNotBlank())
        val exquisite =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.ExquisiteFall,
            )

        assertEquals("red_mist_bites_back", scarlet.id)
        assertTrue(scarlet.text.isNotBlank())
        val labyrinth =
            provider.defaultSample(
                TransportModeOption.Flash,
                AppLanguageOption.English,
                SampleFlavor.LabyrinthOfMutability,
            )

        assertEquals("gold_dust_inhaled", exquisite.id)
        assertTrue(exquisite.text.isNotBlank())
        assertEquals("thread_pulls_the_hero", labyrinth.id)
        assertTrue(labyrinth.text.isNotBlank())
    }

    @Test
    fun `sample ids stay scoped to the active flavor and length`() {
        val provider = AndroidSampleInputTextProvider(context)

        val sampleIds =
            provider.sampleIds(
                mode = TransportModeOption.Flash,
                flavor = SampleFlavor.AncientDynasty,
                length = SampleInputLengthOption.Short,
            )

        assertEquals(
            listOf(
                "alloy_hand_no_warmth",
                "aeonic_ash_stirs",
                "molecular_law_unthreads_flesh",
                "rusted_throne_bad_memory",
                "red_eyes_wear_skin",
            ),
            sampleIds,
        )
    }

    @Test
    fun `sample ids remain stable across themed and ascii lookups for long entries`() {
        val provider = AndroidSampleInputTextProvider(context)

        val themed =
            provider.sampleById(
                mode = TransportModeOption.Flash,
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                sampleId = "ancient_engine_grants_motion",
            )
        val ascii =
            provider.sampleById(
                mode = TransportModeOption.Pro,
                language = AppLanguageOption.English,
                flavor = SampleFlavor.SacredMachine,
                sampleId = "ancient_engine_grants_motion",
            )

        assertEquals("ancient_engine_grants_motion", themed?.id)
        assertTrue(themed?.text.orEmpty().isNotBlank())
        assertEquals("ancient_engine_grants_motion", ascii?.id)
        assertTrue(ascii?.text.orEmpty().isNotBlank())
        assertTrue(ascii?.text.orEmpty().all { it.code in 0..0x7F })
    }
}
