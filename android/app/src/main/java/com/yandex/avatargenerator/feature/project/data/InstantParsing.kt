package com.yandex.avatargenerator.feature.project.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Разбирает время из ответа API (`created_at`). Один парсер на всю фичу:
 * бэкенд сериализует время по-разному в разных хендлерах, и расходиться в
 * трактовке между мапперами нельзя.
 *
 * `Instant.parse` берёт формы с `Z` и с офсетом (`+03:00`); дату без зоны
 * (`2026-08-14T10:00:00`) он не принимает — её трактуем как UTC (бэкенд хранит
 * время в UTC). Совсем кривой формат не роняет разбор — вернём `null`: время
 * это подпись под сообщением, из-за неё терять весь ответ незачем.
 */
internal fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }
        .recoverCatching { LocalDateTime.parse(this).toInstant(ZoneOffset.UTC) }
        .getOrNull()
