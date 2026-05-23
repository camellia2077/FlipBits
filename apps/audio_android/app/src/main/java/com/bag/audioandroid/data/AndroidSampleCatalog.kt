package com.bag.audioandroid.data

import androidx.annotation.StringRes
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption

internal data class AndroidSampleCatalogEntry(
    val id: String,
    val length: SampleInputLengthOption,
    @param:StringRes val resId: Int,
)

internal object AndroidSampleCatalog {
    // Sample resources follow the explicit key contract
    // audio_sample_<flavor>_<family>_<length>_<slug>.
    // `length` is modeled here as data and must not be inferred from XML order.
    // Keep the static sample catalog separate from the runtime provider so
    // content growth does not obscure selection and localization behavior.
    fun sampleEntries(
        flavor: SampleFlavor,
        mode: TransportModeOption,
        length: SampleInputLengthOption,
    ): List<AndroidSampleCatalogEntry> =
        when (flavor) {
            SampleFlavor.SacredMachine -> sacredMachineSampleEntries(mode, length)
            SampleFlavor.AncientDynasty -> ancientDynastySampleEntries(mode, length)
            SampleFlavor.ImmortalRot -> immortalRotSampleEntries(mode, length)
            SampleFlavor.ScarletCarnage -> scarletCarnageSampleEntries(mode, length)
            SampleFlavor.ExquisiteFall -> exquisiteFallSampleEntries(mode, length)
            SampleFlavor.LabyrinthOfMutability -> labyrinthOfMutabilitySampleEntries(mode, length)
        }

    fun allSampleEntries(
        flavor: SampleFlavor,
        mode: TransportModeOption,
    ): List<AndroidSampleCatalogEntry> =
        when (flavor) {
            SampleFlavor.SacredMachine -> sacredMachineSampleEntries(mode)
            SampleFlavor.AncientDynasty -> ancientDynastySampleEntries(mode)
            SampleFlavor.ImmortalRot -> immortalRotSampleEntries(mode)
            SampleFlavor.ScarletCarnage -> scarletCarnageSampleEntries(mode)
            SampleFlavor.ExquisiteFall -> exquisiteFallSampleEntries(mode)
            SampleFlavor.LabyrinthOfMutability -> labyrinthOfMutabilitySampleEntries(mode)
        }

    private fun ancientDynastySampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.ancientDynastySampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun sacredMachineSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.sacredMachineSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun immortalRotSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.immortalRotSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun scarletCarnageSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.scarletCarnageSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun exquisiteFallSampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.exquisiteFallSampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private fun labyrinthOfMutabilitySampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption? = null,
    ): List<AndroidSampleCatalogEntry> =
        CatalogDefinitions.labyrinthOfMutabilitySampleCatalog
            .asSequence()
            .filter { definition -> length == null || length == definition.length }
            .map { definition ->
                AndroidSampleCatalogEntry(
                    id = definition.id,
                    length = definition.length,
                    resId = definition.resIdFor(mode),
                )
            }.toList()

    private enum class AncientDynastyThemeCategory {
        SomaticStripping,
        AeonicSleepAwakening,
        AbsoluteMaterialism,
        MindDecayAristocracy,
        CurseExtremeAlienation,
    }

    private enum class SacredMachineThemeCategory {
        RiteOfMaintenance,
        EngineAwakening,
        SignalLitany,
        ForgeChronicle,
        FleshTranscendence,
        OriginPilgrimage,
        AbyssalQuarantine,
        PurgeCalculus,
    }

    private enum class ImmortalRotThemeCategory {
        FesteringBloom,
        BenevolentContagion,
        LethargicEmbrace,
        EntropicChime,
    }

    private enum class ScarletCarnageThemeCategory {
        CrimsonFrenzy,
        OssuaryTribute,
        IronCredo,
        BrassInferno,
    }

    private enum class ExquisiteFallThemeCategory {
        TrapOfAccumulation,
        VoidOfConsumption,
        DissolutionOfEgo,
        SolipsisticApex,
        TyrannyOfPerfection,
        SeductionOfStandard,
    }

    private enum class LabyrinthOfMutabilityThemeCategory {
        FractalConspiracy,
        ParadoxArcanum,
        KaleidoscopeFlesh,
        AbyssalArchives,
    }

    private data class SacredMachineSampleDefinition(
        val id: String,
        val themeCategory: SacredMachineThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        // Pro/Mini samples resolve through the shared ASCII baseline and keep the
        // same explicit short/long semantics as the themed resource keys.
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private data class AncientDynastySampleDefinition(
        val id: String,
        val themeCategory: AncientDynastyThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private data class ImmortalRotSampleDefinition(
        val id: String,
        val themeCategory: ImmortalRotThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private data class ScarletCarnageSampleDefinition(
        val id: String,
        val themeCategory: ScarletCarnageThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private data class ExquisiteFallSampleDefinition(
        val id: String,
        val themeCategory: ExquisiteFallThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private data class LabyrinthOfMutabilitySampleDefinition(
        val id: String,
        val themeCategory: LabyrinthOfMutabilityThemeCategory,
        val length: SampleInputLengthOption,
        @param:StringRes val themedResId: Int,
        @param:StringRes val asciiResId: Int,
    ) {
        fun resIdFor(mode: TransportModeOption): Int =
            when (mode) {
                TransportModeOption.Flash, TransportModeOption.Ultra -> themedResId
                TransportModeOption.Pro, TransportModeOption.Mini -> asciiResId
            }
    }

    private object CatalogDefinitions {
        val sacredMachineSampleCatalog =
            listOf(
                SacredMachineSampleDefinition(
                    id = "caliper_oil_rite",
                    themeCategory = SacredMachineThemeCategory.RiteOfMaintenance,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_caliper_oil_rite,
                    asciiResId = R.string.audio_sample_pro_ascii_short_caliper_oil_rite,
                ),
                SacredMachineSampleDefinition(
                    id = "bolt_sequence_litany",
                    themeCategory = SacredMachineThemeCategory.RiteOfMaintenance,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_bolt_sequence_litany,
                    asciiResId = R.string.audio_sample_pro_ascii_long_bolt_sequence_litany,
                ),
                SacredMachineSampleDefinition(
                    id = "plasma_matrix_wakes",
                    themeCategory = SacredMachineThemeCategory.EngineAwakening,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_plasma_matrix_wakes,
                    asciiResId = R.string.audio_sample_pro_ascii_short_plasma_matrix_wakes,
                ),
                SacredMachineSampleDefinition(
                    id = "ancient_engine_grants_motion",
                    themeCategory = SacredMachineThemeCategory.EngineAwakening,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_ancient_engine_grants_motion,
                    asciiResId = R.string.audio_sample_pro_ascii_long_ancient_engine_grants_motion,
                ),
                SacredMachineSampleDefinition(
                    id = "ping_canticle_confirmed",
                    themeCategory = SacredMachineThemeCategory.SignalLitany,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_ping_canticle_confirmed,
                    asciiResId = R.string.audio_sample_pro_ascii_short_ping_canticle_confirmed,
                ),
                SacredMachineSampleDefinition(
                    id = "deep_array_handshake",
                    themeCategory = SacredMachineThemeCategory.SignalLitany,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_deep_array_handshake,
                    asciiResId = R.string.audio_sample_pro_ascii_long_deep_array_handshake,
                ),
                SacredMachineSampleDefinition(
                    id = "serials_from_red_steel",
                    themeCategory = SacredMachineThemeCategory.ForgeChronicle,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_serials_from_red_steel,
                    asciiResId = R.string.audio_sample_pro_ascii_short_serials_from_red_steel,
                ),
                SacredMachineSampleDefinition(
                    id = "endless_quota_chronicle",
                    themeCategory = SacredMachineThemeCategory.ForgeChronicle,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_endless_quota_chronicle,
                    asciiResId = R.string.audio_sample_pro_ascii_long_endless_quota_chronicle,
                ),
                SacredMachineSampleDefinition(
                    id = "nerve_cut_ascension",
                    themeCategory = SacredMachineThemeCategory.FleshTranscendence,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_nerve_cut_ascension,
                    asciiResId = R.string.audio_sample_pro_ascii_short_nerve_cut_ascension,
                ),
                SacredMachineSampleDefinition(
                    id = "chromium_spine_rite",
                    themeCategory = SacredMachineThemeCategory.FleshTranscendence,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_chromium_spine_rite,
                    asciiResId = R.string.audio_sample_pro_ascii_long_chromium_spine_rite,
                ),
                SacredMachineSampleDefinition(
                    id = "source_matrix_recovered",
                    themeCategory = SacredMachineThemeCategory.OriginPilgrimage,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_source_matrix_recovered,
                    asciiResId = R.string.audio_sample_pro_ascii_short_source_matrix_recovered,
                ),
                SacredMachineSampleDefinition(
                    id = "ancient_database_pilgrimage",
                    themeCategory = SacredMachineThemeCategory.OriginPilgrimage,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_ancient_database_pilgrimage,
                    asciiResId = R.string.audio_sample_pro_ascii_long_ancient_database_pilgrimage,
                ),
                SacredMachineSampleDefinition(
                    id = "unpowered_terminal_thinks",
                    themeCategory = SacredMachineThemeCategory.AbyssalQuarantine,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_unpowered_terminal_thinks,
                    asciiResId = R.string.audio_sample_pro_ascii_short_unpowered_terminal_thinks,
                ),
                SacredMachineSampleDefinition(
                    id = "paradox_code_quarantine",
                    themeCategory = SacredMachineThemeCategory.AbyssalQuarantine,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_paradox_code_quarantine,
                    asciiResId = R.string.audio_sample_pro_ascii_long_paradox_code_quarantine,
                ),
                SacredMachineSampleDefinition(
                    id = "kill_confirmed_equation",
                    themeCategory = SacredMachineThemeCategory.PurgeCalculus,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_sacred_machine_themed_short_kill_confirmed_equation,
                    asciiResId = R.string.audio_sample_pro_ascii_short_kill_confirmed_equation,
                ),
                SacredMachineSampleDefinition(
                    id = "ballistic_debug_grid",
                    themeCategory = SacredMachineThemeCategory.PurgeCalculus,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_sacred_machine_themed_long_ballistic_debug_grid,
                    asciiResId = R.string.audio_sample_pro_ascii_long_ballistic_debug_grid,
                ),
            )

        val ancientDynastySampleCatalog =
            listOf(
                AncientDynastySampleDefinition(
                    id = "alloy_hand_no_warmth",
                    themeCategory = AncientDynastyThemeCategory.SomaticStripping,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_short_alloy_hand_no_warmth,
                    asciiResId = R.string.audio_sample_pro_ascii_short_alloy_hand_no_warmth,
                ),
                AncientDynastySampleDefinition(
                    id = "soul_lost_in_emerald_light",
                    themeCategory = AncientDynastyThemeCategory.SomaticStripping,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_long_soul_lost_in_emerald_light,
                    asciiResId = R.string.audio_sample_pro_ascii_long_soul_lost_in_emerald_light,
                ),
                AncientDynastySampleDefinition(
                    id = "aeonic_ash_stirs",
                    themeCategory = AncientDynastyThemeCategory.AeonicSleepAwakening,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_short_aeonic_ash_stirs,
                    asciiResId = R.string.audio_sample_pro_ascii_short_aeonic_ash_stirs,
                ),
                AncientDynastySampleDefinition(
                    id = "obsidian_obelisks_rise",
                    themeCategory = AncientDynastyThemeCategory.AeonicSleepAwakening,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_long_obsidian_obelisks_rise,
                    asciiResId = R.string.audio_sample_pro_ascii_long_obsidian_obelisks_rise,
                ),
                AncientDynastySampleDefinition(
                    id = "molecular_law_unthreads_flesh",
                    themeCategory = AncientDynastyThemeCategory.AbsoluteMaterialism,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_short_molecular_law_unthreads_flesh,
                    asciiResId = R.string.audio_sample_pro_ascii_short_molecular_law_unthreads_flesh,
                ),
                AncientDynastySampleDefinition(
                    id = "false_gods_reduced_to_batteries",
                    themeCategory = AncientDynastyThemeCategory.AbsoluteMaterialism,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_long_false_gods_reduced_to_batteries,
                    asciiResId = R.string.audio_sample_pro_ascii_long_false_gods_reduced_to_batteries,
                ),
                AncientDynastySampleDefinition(
                    id = "rusted_throne_bad_memory",
                    themeCategory = AncientDynastyThemeCategory.MindDecayAristocracy,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_short_rusted_throne_bad_memory,
                    asciiResId = R.string.audio_sample_pro_ascii_short_rusted_throne_bad_memory,
                ),
                AncientDynastySampleDefinition(
                    id = "court_debates_dead_realm",
                    themeCategory = AncientDynastyThemeCategory.MindDecayAristocracy,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_long_court_debates_dead_realm,
                    asciiResId = R.string.audio_sample_pro_ascii_long_court_debates_dead_realm,
                ),
                AncientDynastySampleDefinition(
                    id = "red_eyes_wear_skin",
                    themeCategory = AncientDynastyThemeCategory.CurseExtremeAlienation,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_short_red_eyes_wear_skin,
                    asciiResId = R.string.audio_sample_pro_ascii_short_red_eyes_wear_skin,
                ),
                AncientDynastySampleDefinition(
                    id = "extinction_logic_harvests_life",
                    themeCategory = AncientDynastyThemeCategory.CurseExtremeAlienation,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_ancient_dynasty_themed_long_extinction_logic_harvests_life,
                    asciiResId = R.string.audio_sample_pro_ascii_long_extinction_logic_harvests_life,
                ),
            )

        val immortalRotSampleCatalog =
            listOf(
                ImmortalRotSampleDefinition(
                    id = "mushrooms_from_empty_eyes",
                    themeCategory = ImmortalRotThemeCategory.FesteringBloom,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_immortal_rot_themed_short_mushrooms_from_empty_eyes,
                    asciiResId = R.string.audio_sample_pro_ascii_short_mushrooms_from_empty_eyes,
                ),
                ImmortalRotSampleDefinition(
                    id = "harvest_beneath_the_flowers",
                    themeCategory = ImmortalRotThemeCategory.FesteringBloom,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_immortal_rot_themed_long_harvest_beneath_the_flowers,
                    asciiResId = R.string.audio_sample_pro_ascii_long_harvest_beneath_the_flowers,
                ),
                ImmortalRotSampleDefinition(
                    id = "fever_shared_as_bread",
                    themeCategory = ImmortalRotThemeCategory.BenevolentContagion,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_immortal_rot_themed_short_fever_shared_as_bread,
                    asciiResId = R.string.audio_sample_pro_ascii_short_fever_shared_as_bread,
                ),
                ImmortalRotSampleDefinition(
                    id = "kind_contagion_gathers_family",
                    themeCategory = ImmortalRotThemeCategory.BenevolentContagion,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_immortal_rot_themed_long_kind_contagion_gathers_family,
                    asciiResId = R.string.audio_sample_pro_ascii_long_kind_contagion_gathers_family,
                ),
                ImmortalRotSampleDefinition(
                    id = "warm_mire_stillness",
                    themeCategory = ImmortalRotThemeCategory.LethargicEmbrace,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_immortal_rot_themed_short_warm_mire_stillness,
                    asciiResId = R.string.audio_sample_pro_ascii_short_warm_mire_stillness,
                ),
                ImmortalRotSampleDefinition(
                    id = "shield_sinks_from_hand",
                    themeCategory = ImmortalRotThemeCategory.LethargicEmbrace,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_immortal_rot_themed_long_shield_sinks_from_hand,
                    asciiResId = R.string.audio_sample_pro_ascii_long_shield_sinks_from_hand,
                ),
                ImmortalRotSampleDefinition(
                    id = "rust_bell_in_fog",
                    themeCategory = ImmortalRotThemeCategory.EntropicChime,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_immortal_rot_themed_short_rust_bell_in_fog,
                    asciiResId = R.string.audio_sample_pro_ascii_short_rust_bell_in_fog,
                ),
                ImmortalRotSampleDefinition(
                    id = "patient_rain_takes_the_wall",
                    themeCategory = ImmortalRotThemeCategory.EntropicChime,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_immortal_rot_themed_long_patient_rain_takes_the_wall,
                    asciiResId = R.string.audio_sample_pro_ascii_long_patient_rain_takes_the_wall,
                ),
            )

        val scarletCarnageSampleCatalog =
            listOf(
                ScarletCarnageSampleDefinition(
                    id = "red_mist_bites_back",
                    themeCategory = ScarletCarnageThemeCategory.CrimsonFrenzy,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_short_red_mist_bites_back,
                    asciiResId = R.string.audio_sample_pro_ascii_short_red_mist_bites_back,
                ),
                ScarletCarnageSampleDefinition(
                    id = "reason_breaks_blood_runs",
                    themeCategory = ScarletCarnageThemeCategory.CrimsonFrenzy,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_long_reason_breaks_blood_runs,
                    asciiResId = R.string.audio_sample_pro_ascii_long_reason_breaks_blood_runs,
                ),
                ScarletCarnageSampleDefinition(
                    id = "skull_step_receives",
                    themeCategory = ScarletCarnageThemeCategory.OssuaryTribute,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_short_skull_step_receives,
                    asciiResId = R.string.audio_sample_pro_ascii_short_skull_step_receives,
                ),
                ScarletCarnageSampleDefinition(
                    id = "red_river_pays_the_stair",
                    themeCategory = ScarletCarnageThemeCategory.OssuaryTribute,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_long_red_river_pays_the_stair,
                    asciiResId = R.string.audio_sample_pro_ascii_long_red_river_pays_the_stair,
                ),
                ScarletCarnageSampleDefinition(
                    id = "black_iron_answers_magic",
                    themeCategory = ScarletCarnageThemeCategory.IronCredo,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_short_black_iron_answers_magic,
                    asciiResId = R.string.audio_sample_pro_ascii_short_black_iron_answers_magic,
                ),
                ScarletCarnageSampleDefinition(
                    id = "face_to_face_truth",
                    themeCategory = ScarletCarnageThemeCategory.IronCredo,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_long_face_to_face_truth,
                    asciiResId = R.string.audio_sample_pro_ascii_long_face_to_face_truth,
                ),
                ScarletCarnageSampleDefinition(
                    id = "brass_gears_drink_blood",
                    themeCategory = ScarletCarnageThemeCategory.BrassInferno,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_short_brass_gears_drink_blood,
                    asciiResId = R.string.audio_sample_pro_ascii_short_brass_gears_drink_blood,
                ),
                ScarletCarnageSampleDefinition(
                    id = "war_engine_eats_the_sky",
                    themeCategory = ScarletCarnageThemeCategory.BrassInferno,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_scarlet_carnage_themed_long_war_engine_eats_the_sky,
                    asciiResId = R.string.audio_sample_pro_ascii_long_war_engine_eats_the_sky,
                ),
            )

        val exquisiteFallSampleCatalog =
            listOf(
                ExquisiteFallSampleDefinition(
                    id = "gold_dust_inhaled",
                    themeCategory = ExquisiteFallThemeCategory.TrapOfAccumulation,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_gold_dust_inhaled,
                    asciiResId = R.string.audio_sample_pro_ascii_short_gold_dust_inhaled,
                ),
                ExquisiteFallSampleDefinition(
                    id = "vault_without_enough",
                    themeCategory = ExquisiteFallThemeCategory.TrapOfAccumulation,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_vault_without_enough,
                    asciiResId = R.string.audio_sample_pro_ascii_long_vault_without_enough,
                ),
                ExquisiteFallSampleDefinition(
                    id = "sweet_meat_never_ends",
                    themeCategory = ExquisiteFallThemeCategory.VoidOfConsumption,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_sweet_meat_never_ends,
                    asciiResId = R.string.audio_sample_pro_ascii_short_sweet_meat_never_ends,
                ),
                ExquisiteFallSampleDefinition(
                    id = "banquet_eats_the_tongue",
                    themeCategory = ExquisiteFallThemeCategory.VoidOfConsumption,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_banquet_eats_the_tongue,
                    asciiResId = R.string.audio_sample_pro_ascii_long_banquet_eats_the_tongue,
                ),
                ExquisiteFallSampleDefinition(
                    id = "velvet_barbs_kiss",
                    themeCategory = ExquisiteFallThemeCategory.DissolutionOfEgo,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_velvet_barbs_kiss,
                    asciiResId = R.string.audio_sample_pro_ascii_short_velvet_barbs_kiss,
                ),
                ExquisiteFallSampleDefinition(
                    id = "self_dissolves_in_touch",
                    themeCategory = ExquisiteFallThemeCategory.DissolutionOfEgo,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_self_dissolves_in_touch,
                    asciiResId = R.string.audio_sample_pro_ascii_long_self_dissolves_in_touch,
                ),
                ExquisiteFallSampleDefinition(
                    id = "crown_too_heavy",
                    themeCategory = ExquisiteFallThemeCategory.SolipsisticApex,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_crown_too_heavy,
                    asciiResId = R.string.audio_sample_pro_ascii_short_crown_too_heavy,
                ),
                ExquisiteFallSampleDefinition(
                    id = "dream_throne_commands_all",
                    themeCategory = ExquisiteFallThemeCategory.SolipsisticApex,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_dream_throne_commands_all,
                    asciiResId = R.string.audio_sample_pro_ascii_long_dream_throne_commands_all,
                ),
                ExquisiteFallSampleDefinition(
                    id = "mirror_keeps_perfection",
                    themeCategory = ExquisiteFallThemeCategory.TyrannyOfPerfection,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_mirror_keeps_perfection,
                    asciiResId = R.string.audio_sample_pro_ascii_short_mirror_keeps_perfection,
                ),
                ExquisiteFallSampleDefinition(
                    id = "statue_of_self_praise",
                    themeCategory = ExquisiteFallThemeCategory.TyrannyOfPerfection,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_statue_of_self_praise,
                    asciiResId = R.string.audio_sample_pro_ascii_long_statue_of_self_praise,
                ),
                ExquisiteFallSampleDefinition(
                    id = "soft_moss_says_enough",
                    themeCategory = ExquisiteFallThemeCategory.SeductionOfStandard,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_short_soft_moss_says_enough,
                    asciiResId = R.string.audio_sample_pro_ascii_short_soft_moss_says_enough,
                ),
                ExquisiteFallSampleDefinition(
                    id = "lullaby_closes_the_eyes",
                    themeCategory = ExquisiteFallThemeCategory.SeductionOfStandard,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_exquisite_fall_themed_long_lullaby_closes_the_eyes,
                    asciiResId = R.string.audio_sample_pro_ascii_long_lullaby_closes_the_eyes,
                ),
            )

        val labyrinthOfMutabilitySampleCatalog =
            listOf(
                LabyrinthOfMutabilitySampleDefinition(
                    id = "thread_pulls_the_hero",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.FractalConspiracy,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_short_thread_pulls_the_hero,
                    asciiResId = R.string.audio_sample_pro_ascii_short_thread_pulls_the_hero,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "final_trap_needs_rebellion",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.FractalConspiracy,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_long_final_trap_needs_rebellion,
                    asciiResId = R.string.audio_sample_pro_ascii_long_final_trap_needs_rebellion,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "cold_fire_folds_space",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.ParadoxArcanum,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_short_cold_fire_folds_space,
                    asciiResId = R.string.audio_sample_pro_ascii_short_cold_fire_folds_space,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "laws_unwrite_themselves",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.ParadoxArcanum,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_long_laws_unwrite_themselves,
                    asciiResId = R.string.audio_sample_pro_ascii_long_laws_unwrite_themselves,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "new_eye_blesses_spine",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.KaleidoscopeFlesh,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_short_new_eye_blesses_spine,
                    asciiResId = R.string.audio_sample_pro_ascii_short_new_eye_blesses_spine,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "flesh_refuses_one_shape",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.KaleidoscopeFlesh,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_long_flesh_refuses_one_shape,
                    asciiResId = R.string.audio_sample_pro_ascii_long_flesh_refuses_one_shape,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "library_bleeds_prophecy",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.AbyssalArchives,
                    length = SampleInputLengthOption.Short,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_short_library_bleeds_prophecy,
                    asciiResId = R.string.audio_sample_pro_ascii_short_library_bleeds_prophecy,
                ),
                LabyrinthOfMutabilitySampleDefinition(
                    id = "truth_crushes_the_seer",
                    themeCategory = LabyrinthOfMutabilityThemeCategory.AbyssalArchives,
                    length = SampleInputLengthOption.Long,
                    themedResId = R.string.audio_sample_labyrinth_of_mutability_themed_long_truth_crushes_the_seer,
                    asciiResId = R.string.audio_sample_pro_ascii_long_truth_crushes_the_seer,
                ),
            )
    }
}
