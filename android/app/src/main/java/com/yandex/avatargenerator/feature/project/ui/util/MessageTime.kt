package com.yandex.avatargenerator.feature.project.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Подпись времени сообщения («11:32» или «11:32 AM») — формат и порядок задаёт
 * локаль приложения, поэтому свой шаблон вида `HH:mm` здесь не нужен.
 *
 * Локаль берём из [LocalConfiguration] (её на корне Compose подменяет выбранный
 * язык), а не из `Locale.getDefault()` — иначе формат шёл бы за системной локалью
 * и расходился бы с остальным UI при языке приложения ≠ системного.
 *
 * @param instant время сообщения; `null` (бэкенд не прислал `created_at` или
 *  прислал в неизвестном формате) даёт `null` — пузырь рисуется без подписи.
 */
@Composable
internal fun rememberMessageTimeLabel(instant: Instant?): String? {
    val locale = LocalConfiguration.current.locales.get(0)
    return remember(instant, locale) {
        instant?.let {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(it)
        }
    }
}
