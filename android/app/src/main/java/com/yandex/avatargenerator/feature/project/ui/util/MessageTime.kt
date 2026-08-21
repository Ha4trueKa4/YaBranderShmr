package com.yandex.avatargenerator.feature.project.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Подпись времени сообщения («11:32» или «11:32 AM») — формат и порядок задаёт
 * локаль устройства, поэтому свой шаблон вида `HH:mm` здесь не нужен.
 *
 * Живёт в `ui/`, а не в `domain/`: зона и локаль — свойства окружения, а не данных.
 *
 * @param instant время сообщения; `null` (бэкенд не прислал `created_at` или
 *  прислал в неизвестном формате) даёт `null` — пузырь рисуется без подписи.
 */
@Composable
internal fun rememberMessageTimeLabel(instant: Instant?): String? = remember(instant) {
    instant?.let {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(it)
    }
}
