package com.yandex.avatargenerator.feature.settings.domain.model

/**
 * Режим темы оформления, выбранный пользователем в настройках.
 *
 * Разрешение [SYSTEM] в конкретную (светлую/тёмную) требует знать системную
 * настройку — её отдаёт UI (`isSystemInDarkTheme`), поэтому [isDark] принимает
 * её параметром, а сам enum остаётся без зависимостей от Android.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Тёмная ли тема при данном системном значении (актуально только для [SYSTEM]). */
    fun isDark(systemInDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDark
        LIGHT -> false
        DARK -> true
    }
}
