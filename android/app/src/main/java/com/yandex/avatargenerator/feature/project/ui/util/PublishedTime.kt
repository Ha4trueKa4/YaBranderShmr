package com.yandex.avatargenerator.feature.project.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.yandex.avatargenerator.R
import java.time.Duration
import java.time.Instant

/**
 * Относительная подпись публикации («Published 3 days ago»). Живёт в `ui/`, а не
 * в `domain/`: это отображение, а не свойство данных (ср. [rememberMessageTimeLabel]).
 *
 * Секунды/минуты/часы не склоняются — обычные строки; дни — plurals. `null`
 * (время неизвестно) → нейтральное «Recently published».
 */
@Composable
internal fun publishedLabel(instant: Instant?, now: Instant = Instant.now()): String {
    if (instant == null) return stringResource(R.string.public_project_published_recently)
    val minutes = Duration.between(instant, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.public_project_published_just_now)
        minutes < 60 -> stringResource(R.string.public_project_published_minutes, minutes.toInt())
        minutes < 60 * 24 -> stringResource(R.string.public_project_published_hours, (minutes / 60).toInt())
        else -> {
            val days = (minutes / (60 * 24)).toInt()
            pluralStringResource(R.plurals.public_project_published_days, days, days)
        }
    }
}
