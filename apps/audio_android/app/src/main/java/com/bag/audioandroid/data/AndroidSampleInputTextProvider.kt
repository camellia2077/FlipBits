package com.bag.audioandroid.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import java.util.Locale
import kotlin.random.Random

class AndroidSampleInputTextProvider(
    private val appContext: Context,
    private val random: Random = Random.Default,
) : SampleInputTextProvider {
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
    ): SampleInput =
        resolveSample(
            resourcesFor(language),
            sampleEntries(mode, SampleInputLengthOption.Short).first(),
        )

    override fun randomSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        length: SampleInputLengthOption,
        excludingSampleId: String?,
    ): SampleInput {
        val entries = sampleEntries(mode, length)
        val candidates =
            if (excludingSampleId != null && entries.size > 1) {
                entries.filterNot { it.id == excludingSampleId }
            } else {
                entries
            }
        val selectedEntry = candidates[random.nextInt(candidates.size)]
        return resolveSample(resourcesFor(language), selectedEntry)
    }

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        sampleId: String,
    ): SampleInput? =
        sampleEntries(mode, SampleInputLengthOption.Short)
            .plus(sampleEntries(mode, SampleInputLengthOption.Long))
            .firstOrNull { it.id == sampleId }
            ?.let { resolveSample(resourcesFor(language), it) }

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(appContext.resources.configuration)
        configuration.setLocale(locale)
        return appContext.createConfigurationContext(configuration)
    }

    private fun resourcesFor(language: AppLanguageOption): Resources =
        when (language) {
            AppLanguageOption.FollowSystem -> {
                val systemLocale = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
                localizedContext(systemLocale).resources
            }
            AppLanguageOption.Chinese -> localizedContext(Locale.SIMPLIFIED_CHINESE).resources
            AppLanguageOption.TraditionalChinese ->
                localizedContext(Locale.forLanguageTag("zh-TW")).resources
            AppLanguageOption.English -> localizedContext(Locale.ENGLISH).resources
            AppLanguageOption.Japanese -> localizedContext(Locale.JAPANESE).resources
            AppLanguageOption.German -> localizedContext(Locale.GERMAN).resources
            AppLanguageOption.Russian -> localizedContext(Locale.forLanguageTag("ru")).resources
            AppLanguageOption.Spanish -> localizedContext(Locale.forLanguageTag("es")).resources
            AppLanguageOption.Portuguese -> localizedContext(Locale.forLanguageTag("pt")).resources
        }

    private fun resolveSample(
        resources: Resources,
        entry: SampleEntry,
    ): SampleInput =
        SampleInput(
            id = entry.id,
            text = resources.getString(entry.resId),
        )

    private fun sampleEntries(
        mode: TransportModeOption,
        length: SampleInputLengthOption,
    ): List<SampleEntry> =
        when (mode) {
            TransportModeOption.Flash, TransportModeOption.Ultra ->
                when (length) {
                    SampleInputLengthOption.Short -> thematicSamples
                    SampleInputLengthOption.Long -> thematicLongSamples
                }

            TransportModeOption.Pro ->
                when (length) {
                    SampleInputLengthOption.Short -> proSamples
                    SampleInputLengthOption.Long -> proLongSamples
                }
        }

    private data class SampleEntry(
        val id: String,
        @param:StringRes val resId: Int,
    )

    private companion object {
        val thematicSamples =
            listOf(
                SampleEntry("old_star_chart", R.string.audio_transport_flash_example),
                SampleEntry("sealed_engine", R.string.audio_sample_thematic_2),
                SampleEntry("seventh_torch", R.string.audio_sample_thematic_3),
                SampleEntry("pilgrim_ship", R.string.audio_sample_thematic_4),
                SampleEntry("iron_bells", R.string.audio_sample_thematic_5),
            )

        val thematicLongSamples =
            listOf(
                SampleEntry("bell_tower_log", R.string.audio_sample_thematic_long_1),
                SampleEntry("gatehouse_watch", R.string.audio_sample_thematic_long_2),
                SampleEntry("furnace_chronicle", R.string.audio_sample_thematic_long_3),
                SampleEntry("pilgrim_record", R.string.audio_sample_thematic_long_4),
            )

        val proSamples =
            listOf(
                SampleEntry("ash_bells", R.string.audio_transport_pro_example),
                SampleEntry("outer_gate", R.string.audio_sample_pro_2),
                SampleEntry("red_keepers", R.string.audio_sample_pro_3),
                SampleEntry("seventh_torch", R.string.audio_sample_pro_4),
                SampleEntry("iron_spires", R.string.audio_sample_pro_5),
            )

        val proLongSamples =
            listOf(
                SampleEntry("bell_tower_log_pro", R.string.audio_sample_pro_long_1),
                SampleEntry("gatehouse_watch_pro", R.string.audio_sample_pro_long_2),
                SampleEntry("furnace_chronicle_pro", R.string.audio_sample_pro_long_3),
                SampleEntry("pilgrim_record_pro", R.string.audio_sample_pro_long_4),
            )
    }
}
