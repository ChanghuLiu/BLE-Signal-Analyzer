package com.ble.signal.analyzer.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun supportedLanguageList_containsEveryRequestedLocale() {
        assertEquals(13, AppLanguage.supportedLanguages.size)
        assertEquals(
            setOf(
                "en", "fr", "de", "es", "pt-BR", "zh-CN", "zh-TW",
                "ja", "ko", "ar", "tr", "id", "hi",
            ),
            AppLanguage.supportedLanguages.mapNotNull(AppLanguage::languageTag).toSet(),
        )
    }

    @Test
    fun emptyLocaleSelection_usesSystemDefault() {
        assertEquals(AppLanguage.SystemDefault, AppLanguage.fromLanguageTags(null))
        assertEquals(AppLanguage.SystemDefault, AppLanguage.fromLanguageTags(""))
        assertNull(AppLanguage.SystemDefault.languageTag)
    }

    @Test
    fun supportedLocaleMapping_acceptsAndroidLanguageTags() {
        assertEquals(AppLanguage.PortugueseBrazil, AppLanguage.fromLanguageTags("pt-BR"))
        assertEquals(AppLanguage.TraditionalChinese, AppLanguage.fromLanguageTags("zh_TW"))
        assertEquals(AppLanguage.Indonesian, AppLanguage.fromLanguageTags("id"))
        assertEquals(AppLanguage.Indonesian, AppLanguage.fromLanguageTags("in"))
    }

    @Test
    fun unsupportedExplicitLocale_fallsBackToEnglish() {
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTags("it-IT"))
        assertTrue(AppLanguage.English in AppLanguage.supportedLanguages)
    }
}
