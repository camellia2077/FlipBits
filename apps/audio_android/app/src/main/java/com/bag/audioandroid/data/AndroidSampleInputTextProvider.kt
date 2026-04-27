package com.bag.audioandroid.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.SampleInputLengthOption
import com.bag.audioandroid.ui.model.TransportModeOption
import java.util.Locale

class AndroidSampleInputTextProvider(
    private val appContext: Context,
) : SampleInputTextProvider {
    // This provider owns runtime concerns only: locale selection, sample id lookup,
    // and resolving the selected sample id into a localized Android string.
    override fun defaultSample(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
    ): SampleInput =
        resolveSample(
            resourcesFor(language),
            AndroidSampleCatalog.sampleEntries(flavor, mode, SampleInputLengthOption.Short).first(),
        )

    override fun sampleIds(
        mode: TransportModeOption,
        flavor: SampleFlavor,
        length: SampleInputLengthOption,
    ): List<String> = AndroidSampleCatalog.sampleEntries(flavor, mode, length).map(AndroidSampleCatalogEntry::id)

    override fun sampleById(
        mode: TransportModeOption,
        language: AppLanguageOption,
        flavor: SampleFlavor,
        sampleId: String,
    ): SampleInput? =
        AndroidSampleCatalog
            .allSampleEntries(flavor, mode)
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
            AppLanguageOption.TraditionalChinese -> localizedContext(Locale.forLanguageTag("zh-TW")).resources
            AppLanguageOption.English -> localizedContext(Locale.ENGLISH).resources
            AppLanguageOption.Japanese -> localizedContext(Locale.JAPANESE).resources
            AppLanguageOption.German -> localizedContext(Locale.GERMAN).resources
            AppLanguageOption.Russian -> localizedContext(Locale.forLanguageTag("ru")).resources
            AppLanguageOption.Spanish -> localizedContext(Locale.forLanguageTag("es")).resources
            AppLanguageOption.Portuguese -> localizedContext(Locale.forLanguageTag("pt-BR")).resources
            AppLanguageOption.Ukrainian -> localizedContext(Locale.forLanguageTag("uk")).resources
            AppLanguageOption.Korean -> localizedContext(Locale.KOREAN).resources
            AppLanguageOption.French -> localizedContext(Locale.FRENCH).resources
            AppLanguageOption.Italian -> localizedContext(Locale.ITALIAN).resources
            AppLanguageOption.Polish -> localizedContext(Locale.forLanguageTag("pl")).resources
            AppLanguageOption.HighGothic -> localizedContext(Locale.forLanguageTag("la")).resources
        }

    private fun resolveSample(
        resources: Resources,
        entry: AndroidSampleCatalogEntry,
    ): SampleInput =
        SampleInput(
            id = entry.id,
            text = resources.getString(entry.resId),
        )
}
