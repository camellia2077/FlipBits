package com.bag.audioandroid.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageOptionTest {
    @Test
    fun `fromLanguageTags maps traditional chinese tags separately`() {
        assertEquals(
            AppLanguageOption.TraditionalChinese,
            AppLanguageOption.fromLanguageTags("zh-TW"),
        )
        assertEquals(
            AppLanguageOption.TraditionalChinese,
            AppLanguageOption.fromLanguageTags("zh-Hant-HK"),
        )
        assertEquals(
            AppLanguageOption.Chinese,
            AppLanguageOption.fromLanguageTags("zh-CN"),
        )
        assertEquals(
            AppLanguageOption.Chinese,
            AppLanguageOption.fromLanguageTags("zh-Hans"),
        )
    }

    @Test
    fun `fromLanguageTags maps newly added locales`() {
        assertEquals(
            AppLanguageOption.Korean,
            AppLanguageOption.fromLanguageTags("ko-KR"),
        )
        assertEquals(
            AppLanguageOption.French,
            AppLanguageOption.fromLanguageTags("fr-FR"),
        )
        assertEquals(
            AppLanguageOption.Italian,
            AppLanguageOption.fromLanguageTags("it-IT"),
        )
        assertEquals(
            AppLanguageOption.Polish,
            AppLanguageOption.fromLanguageTags("pl-PL"),
        )
    }
}
