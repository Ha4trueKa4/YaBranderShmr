package com.yandex.avatargenerator.feature.create

import com.yandex.avatargenerator.feature.create.domain.util.RelativeTimeLabel
import com.yandex.avatargenerator.feature.create.domain.util.relativeTimeLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Границы правил подписи времени. Момент отсчёта фиксированный, часовой пояс —
 * UTC: иначе результат зависел бы от настроек машины, на которой идёт сборка.
 */
internal class RelativeTimeTest {

    private val now: Instant = Instant.parse("2026-08-08T12:00:00Z")
    private val zone: ZoneId = ZoneId.of("UTC")

    private fun labelFor(amount: Long, unit: ChronoUnit): RelativeTimeLabel =
        relativeTimeLabel(instant = now.minus(amount, unit), now = now, zone = zone)

    @Test
    fun `less than a minute is just now`() {
        assertEquals(RelativeTimeLabel.JustNow, labelFor(30, ChronoUnit.SECONDS))
        assertEquals(RelativeTimeLabel.JustNow, labelFor(59, ChronoUnit.SECONDS))
    }

    @Test
    fun `a minute and more is counted in minutes`() {
        assertEquals(RelativeTimeLabel.Minutes(1), labelFor(60, ChronoUnit.SECONDS))
        assertEquals(RelativeTimeLabel.Minutes(59), labelFor(59, ChronoUnit.MINUTES))
    }

    @Test
    fun `an hour and more is counted in hours`() {
        assertEquals(RelativeTimeLabel.Hours(1), labelFor(60, ChronoUnit.MINUTES))
        assertEquals(RelativeTimeLabel.Hours(23), labelFor(23, ChronoUnit.HOURS))
    }

    @Test
    fun `a day and more is counted in days`() {
        assertEquals(RelativeTimeLabel.Days(1), labelFor(24, ChronoUnit.HOURS))
        assertEquals(RelativeTimeLabel.Days(6), labelFor(6, ChronoUnit.DAYS))
    }

    @Test
    fun `a week and older is an absolute date`() {
        assertEquals(RelativeTimeLabel.AbsoluteDate("1 Aug"), labelFor(7, ChronoUnit.DAYS))
        assertEquals(RelativeTimeLabel.AbsoluteDate("12 Jul"), labelFor(27, ChronoUnit.DAYS))
    }

    @Test
    fun `a moment in the future degrades to just now`() {
        val future = now.plus(5, ChronoUnit.MINUTES)

        assertEquals(RelativeTimeLabel.JustNow, relativeTimeLabel(future, now, zone))
    }
}
