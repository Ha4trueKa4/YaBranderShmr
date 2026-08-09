package com.yandex.avatargenerator.feature.create.domain.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Как показать момент времени в строке чата. Промежуточное представление между
 * чистым расчётом ([relativeTimeLabel]) и строковыми ресурсами: благодаря ему
 * расчёт живёт в `domain/` и тестируется обычным unit-тестом, без Android-контекста,
 * а подстановку текста делает
 * [com.yandex.avatargenerator.feature.create.ui.util.rememberRelativeTimeLabel].
 */
internal sealed interface RelativeTimeLabel {

    /** Меньше минуты назад. */
    data object JustNow : RelativeTimeLabel

    /** [value] минут назад, 1..59. */
    data class Minutes(val value: Int) : RelativeTimeLabel

    /** [value] часов назад, 1..23. */
    data class Hours(val value: Int) : RelativeTimeLabel

    /** [value] дней назад, 1..6. */
    data class Days(val value: Int) : RelativeTimeLabel

    /** Старше недели — абсолютная дата вида «12 Jul». */
    data class AbsoluteDate(val text: String) : RelativeTimeLabel
}

/**
 * Расчёт подписи времени по макету: «just now», «15 min ago», «1 hr ago»,
 * «3 days ago», дальше — абсолютная дата.
 *
 * Функция чистая (не `@Composable`, без обращения к системным часам внутри) —
 * иначе её не покрыть unit-тестом.
 *
 * @param instant момент, который подписываем.
 * @param now момент отсчёта.
 * @param zone часовой пояс для абсолютной даты.
 * @return [RelativeTimeLabel.JustNow] и для моментов из будущего: рассинхрон
 *  часов клиента и сервера не должен давать отрицательных «-5 min ago».
 */
internal fun relativeTimeLabel(
    instant: Instant,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): RelativeTimeLabel {
    val elapsed = Duration.between(instant, now)
    val minutes = elapsed.toMinutes()

    return when {
        minutes < 1L -> RelativeTimeLabel.JustNow
        minutes < MINUTES_PER_HOUR -> RelativeTimeLabel.Minutes(minutes.toInt())
        elapsed.toHours() < HOURS_PER_DAY -> RelativeTimeLabel.Hours(elapsed.toHours().toInt())
        elapsed.toDays() < DAYS_PER_WEEK -> RelativeTimeLabel.Days(elapsed.toDays().toInt())
        else -> RelativeTimeLabel.AbsoluteDate(
            // Locale.ENGLISH, а не системная: весь текст экрана английский по макету
            // (см. strings.xml), иначе месяц в дате выбивался бы из общего вида.
            AbsoluteDateFormatter.withZone(zone).format(instant),
        )
    }
}

private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L

private val AbsoluteDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
