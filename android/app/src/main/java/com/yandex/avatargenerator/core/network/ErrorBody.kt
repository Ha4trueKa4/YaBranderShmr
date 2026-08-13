package com.yandex.avatargenerator.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

/** Сбой разбора глотаем: причина ошибки — статус, а не формат тела. */
internal suspend inline fun <reified T> HttpResponse.decodeErrorBodyOrNull(): T? = runCatching {
    val body = bodyAsText()
    if (body.isBlank()) null else ApiJson.decodeFromString<T>(body)
}.getOrNull()
