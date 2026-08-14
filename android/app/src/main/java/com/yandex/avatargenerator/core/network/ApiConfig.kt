package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.BuildConfig

/** Параметры подключения к API. */
object ApiConfig {

    /** Завершающий слэш обязателен — иначе Ktor затрёт последний сегмент пути. */
    val BaseUrl: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    const val RequestTimeoutMillis: Long = 30_000
    const val ConnectTimeoutMillis: Long = 15_000
    const val SocketTimeoutMillis: Long = 30_000
}
