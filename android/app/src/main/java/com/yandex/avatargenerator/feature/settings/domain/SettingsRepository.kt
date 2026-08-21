package com.yandex.avatargenerator.feature.settings.domain

import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Пользовательские настройки приложения: тема оформления и язык интерфейса.
 * Значения переживают перезапуск (хранятся в общем `DataStore`).
 */
interface SettingsRepository {

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    val language: Flow<AppLanguage>

    suspend fun setLanguage(language: AppLanguage)
}
