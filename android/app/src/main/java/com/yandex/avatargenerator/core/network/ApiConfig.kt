package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.BuildConfig

/** Параметры подключения к API. */
object ApiConfig {

    /** Завершающий слэш обязателен — иначе Ktor затрёт последний сегмент пути. */
    val BaseUrl: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    const val RequestTimeoutMillis: Long = 30_000
    const val ConnectTimeoutMillis: Long = 15_000
    const val SocketTimeoutMillis: Long = 30_000

    /**
     * Отдельный таймаут для `POST /projects/{id}/generate`: LLM думает дольше
     * общих 30 секунд. Ставится на самом запросе через `timeout { }` — поднимать
     * ради него потолок всему клиенту нельзя.
     */
    const val GenerationTimeoutMillis: Long = 120_000
}
