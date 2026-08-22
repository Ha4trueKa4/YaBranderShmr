package com.yandex.avatargenerator.feature.settings.domain.model

/**
 * Язык интерфейса, выбранный в настройках.
 *
 * @property tag BCP-47 тег локали для применения к приложению; пустой у [SYSTEM]
 *   («как в системе» — переопределения локали нет).
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    RUSSIAN("ru"),
    ENGLISH("en");

    companion object {
        /** Обратное сопоставление сохранённого тега в enum; неизвестный тег → [SYSTEM]. */
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
