package com.yandex.avatargenerator.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Форматы `created_at`, которые парсер обязан понимать.
 *
 * Важная оговорка про среду: эти тесты идут на десктопном JDK 17, а на
 * устройстве `java.time` берётся из libcore (десугаринг не подключён, minSdk =
 * 26) — на Android 8–13 это OpenJDK 8/11. Разница между ними ровно одна и она
 * тут существенна: `Instant.parse` научился принимать офсет вместо `Z` только в
 * JDK 12. Поэтому зелёный прогон здесь не доказывает работу на Android сам по
 * себе — он проверяет, что первой веткой стоит `OffsetDateTime.parse`, который
 * ведёт себя одинаково на обеих платформах.
 */
class InstantParsingTest {

    @Test
    fun `офсет +00_00 — основной формат бэкенда`() {
        assertEquals(
            Instant.parse("2026-08-16T12:34:56Z"),
            "2026-08-16T12:34:56+00:00".toInstantOrNull(),
        )
    }

    @Test
    fun `ненулевой офсет приводится к UTC`() {
        assertEquals(
            Instant.parse("2026-08-14T07:00:00Z"),
            "2026-08-14T10:00:00+03:00".toInstantOrNull(),
        )
    }

    @Test
    fun `форма с Z тоже разбирается`() {
        assertEquals(
            Instant.parse("2026-08-13T12:00:00Z"),
            "2026-08-13T12:00:00Z".toInstantOrNull(),
        )
    }

    @Test
    fun `дата без зоны трактуется как UTC`() {
        assertEquals(
            Instant.parse("2026-08-14T10:00:00Z"),
            "2026-08-14T10:00:00".toInstantOrNull(),
        )
    }

    @Test
    fun `дробные секунды не мешают разбору`() {
        assertEquals(
            Instant.parse("2026-08-16T12:34:56.123Z"),
            "2026-08-16T12:34:56.123+00:00".toInstantOrNull(),
        )
    }

    @Test
    fun `микросекунды из Postgres разбираются`() {
        // История чата сериализуется не хендлером, а самой базой:
        // jsonb_build_object('created_at', NOW()) даёт микросекунды, причём
        // хвостовые нули Postgres обрезает — отсюда две формы дробной части.
        assertEquals(
            Instant.parse("2026-08-16T09:12:33.123456Z"),
            "2026-08-16T09:12:33.123456+00:00".toInstantOrNull(),
        )
        assertEquals(
            Instant.parse("2026-08-16T09:12:40.500Z"),
            "2026-08-16T09:12:40.5+00:00".toInstantOrNull(),
        )
    }

    @Test
    fun `кривой формат даёт null, а не исключение`() {
        assertNull("вчера".toInstantOrNull())
        assertNull("".toInstantOrNull())
        assertNull("2026-08-16".toInstantOrNull())
    }
}
