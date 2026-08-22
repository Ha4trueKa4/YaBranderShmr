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

    /**
     * Последнее прочитанное значение темы для синхронного доступа — нужно как
     * `initialValue` первого кадра (открытие экрана настроек, холодный старт), чтобы
     * тема/селектор не мигали системным значением, пока асинхронный [themeMode] ещё не
     * отдал сохранённый выбор. До первого чтения — [ThemeMode.SYSTEM].
     */
    val lastThemeMode: ThemeMode

    suspend fun setThemeMode(mode: ThemeMode)

    val language: Flow<AppLanguage>

    /**
     * Последний прочитанный язык для синхронного доступа — `initialValue` первого
     * кадра, чтобы он не мигал системной локалью, пока асинхронный [language] ещё не
     * отдал сохранённый выбор. До первого чтения — [AppLanguage.SYSTEM].
     */
    val lastLanguage: AppLanguage

    suspend fun setLanguage(language: AppLanguage)
}
