package com.yandex.avatargenerator.core.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Разбирает время из ответа API (`created_at`). Один парсер на всё приложение:
 * бэкенд сериализует время по-разному в разных хендлерах, и расходиться в
 * трактовке между мапперами нельзя.
 *
 * Порядок веток важен. Основной формат бэкенда — с явным офсетом
 * (`2026-08-16T12:34:56+00:00`), и разбирает его именно `OffsetDateTime.parse`.
 * `Instant.parse` для этого не годится: офсет вместо `Z` он принимает только
 * начиная с JDK 12, а на Android `java.time` берётся из libcore устройства —
 * на Android 8–13 это OpenJDK 8/11, где такая строка падает с
 * `DateTimeParseException`. Десугаринг в проекте не подключён, minSdk = 26,
 * так что подменить реализацию нечем. Unit-тесты этого не ловят: они идут на
 * десктопном JDK 17, где послабление уже есть.
 *
 * Дальше — запасные формы: `Z` (`Instant.parse`) и дата без зоны
 * (`2026-08-14T10:00:00`), которую трактуем как UTC, потому что бэкенд хранит
 * время в UTC. Совсем кривой формат не роняет разбор — вернём `null`: время
 * это подпись под сообщением, из-за неё терять весь ответ незачем.
 */
internal fun String.toInstantOrNull(): Instant? =
    runCatching { OffsetDateTime.parse(this).toInstant() }
        .recoverCatching { Instant.parse(this) }
        .recoverCatching { LocalDateTime.parse(this).toInstant(ZoneOffset.UTC) }
        .getOrNull()
