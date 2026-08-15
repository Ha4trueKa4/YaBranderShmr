package com.yandex.avatargenerator.core.network

import io.ktor.http.HttpMethod

/**
 * Можно ли безопасно повторить запрос. Только чтение: шлюз мог вернуть 5xx уже
 * после того, как бэкенд принял `POST`, и ретрай создал бы вторую генерацию.
 */
internal fun HttpMethod.isRetryable(): Boolean =
    this == HttpMethod.Get || this == HttpMethod.Head
