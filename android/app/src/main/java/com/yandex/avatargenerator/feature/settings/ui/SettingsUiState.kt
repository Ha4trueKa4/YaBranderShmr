package com.yandex.avatargenerator.feature.settings.ui

import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode

/**
 * Состояние экрана настроек. Экран всегда отрисовывается (без loading/error):
 * данные — из локального `DataStore` и уже прочитанной сессии.
 *
 * @param nickname никнейм текущего пользователя без «@» (UI добавит сам); пусто,
 *   если сессия ещё не подтянулась.
 */
data class SettingsUiState(
    val nickname: String,
    val email: String,
    val themeMode: ThemeMode,
    val language: AppLanguage,
    val appVersion: String,
) {
    companion object {
        val Initial = SettingsUiState(
            nickname = "",
            email = "",
            themeMode = ThemeMode.SYSTEM,
            language = AppLanguage.SYSTEM,
            appVersion = "",
        )
    }
}
