package com.ble.signal.analyzer.localization

enum class AppLanguage(val languageTag: String?) {
    SystemDefault(null),
    English("en"),
    French("fr"),
    German("de"),
    Spanish("es-ES"),
    PortugueseBrazil("pt-BR"),
    SimplifiedChinese("zh-CN"),
    TraditionalChinese("zh-TW"),
    Japanese("ja"),
    Korean("ko"),
    Arabic("ar"),
    Turkish("tr"),
    Indonesian("id"),
    Hindi("hi"),
    ;

    companion object {
        val supportedLanguages: List<AppLanguage> = entries.filterNot {
            it == SystemDefault
        }

        fun fromLanguageTags(languageTags: String?): AppLanguage {
            if (languageTags.isNullOrBlank()) return SystemDefault
            val requestedTag = languageTags.substringBefore(',').trim()
                .replace('_', '-')
            if (requestedTag.equals("in", ignoreCase = true)) return Indonesian
            if (requestedTag.equals("es", ignoreCase = true)) return Spanish
            return supportedLanguages.firstOrNull {
                it.languageTag.equals(requestedTag, ignoreCase = true)
            } ?: English
        }
    }
}
