package com.bag.audioandroid.ui.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bag.audioandroid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AppLocaleResourcesTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `extended locales resolve their own translated resources`() {
        val cases =
            listOf(
                AppLanguageOption.German to "Wähle die App-Sprache",
                AppLanguageOption.Russian to "Выберите язык приложения",
                AppLanguageOption.Spanish to "Elige el idioma de la aplicación",
                AppLanguageOption.Portuguese to "Escolha o idioma do aplicativo",
            )

        cases.forEach { (language, expectedSubtitle) ->
            assertEquals(
                expectedSubtitle,
                localizedString(language, R.string.config_language_subtitle),
            )
        }
    }

    private fun localizedString(
        language: AppLanguageOption,
        resId: Int,
    ): String {
        val configuration = appContext.resources.configuration
        val localizedConfiguration = android.content.res.Configuration(configuration)
        localizedConfiguration.setLocale(Locale.forLanguageTag(language.languageTag))
        return appContext.createConfigurationContext(localizedConfiguration).getString(resId)
    }
}
