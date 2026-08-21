package com.yandex.avatargenerator.feature.settings.data

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ключи настроек в общем `DataStore`: режим темы (по имени [com.yandex.avatargenerator
 * .feature.settings.domain.model.ThemeMode]) и язык (BCP-47 тег; пустой — «как в системе»).
 */
internal object SettingsPreferences {
    val THEME_MODE = stringPreferencesKey("settings.theme_mode")
    val LANGUAGE = stringPreferencesKey("settings.language")
}
