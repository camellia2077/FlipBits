package com.bag.audioandroid.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.bag.audioandroid.ui.model.AppLanguageOption
import com.bag.audioandroid.ui.model.TransportModeOption
import java.util.Locale

class AndroidSampleInputTextProvider(
    private val appContext: Context
) {
    fun sampleText(
        mode: TransportModeOption,
        language: AppLanguageOption
    ): String {
        val resources = when (language) {
            AppLanguageOption.FollowSystem -> {
                val systemLocale = Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
                localizedContext(systemLocale).resources
            }
            AppLanguageOption.Chinese -> localizedContext(Locale.SIMPLIFIED_CHINESE).resources
            AppLanguageOption.English -> localizedContext(Locale.ENGLISH).resources
            AppLanguageOption.Japanese -> localizedContext(Locale.JAPANESE).resources
        }
        return resources.getString(mode.exampleTextResId)
    }

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(appContext.resources.configuration)
        configuration.setLocale(locale)
        return appContext.createConfigurationContext(configuration)
    }
}
