package com.yandex.avatargenerator.feature.create.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.create.domain.util.RelativeTimeLabel
import com.yandex.avatargenerator.feature.create.domain.util.relativeTimeLabel
import java.time.Instant

/**
 * Готовая к показу подпись времени: доменный расчёт
 * ([com.yandex.avatargenerator.feature.create.domain.util.relativeTimeLabel])
 * плюс строковые ресурсы. Обращение к ресурсам живёт в `ui/`, чтобы `domain/`
 * не зависел от Android.
 */
@Composable
internal fun rememberRelativeTimeLabel(instant: Instant, now: Instant = Instant.now()): String {
    val label = remember(instant, now) { relativeTimeLabel(instant, now) }
    return when (label) {
        RelativeTimeLabel.JustNow -> stringResource(R.string.chats_time_just_now)
        is RelativeTimeLabel.Minutes -> stringResource(R.string.chats_time_minutes, label.value)
        is RelativeTimeLabel.Hours -> stringResource(R.string.chats_time_hours, label.value)
        is RelativeTimeLabel.Days ->
            pluralStringResource(R.plurals.chats_time_days, label.value, label.value)

        is RelativeTimeLabel.AbsoluteDate -> label.text
    }
}
