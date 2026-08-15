package com.yandex.avatargenerator.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * Общий на всё приложение [OkHttpClient] — используется и Ktor-клиентом
 * ([HttpClientFactory]), и Coil, чтобы у API и загрузки картинок был один
 * пул соединений и общие таймауты.
 */
fun createSharedOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(ApiConfig.ConnectTimeoutMillis, TimeUnit.MILLISECONDS)
    .readTimeout(ApiConfig.SocketTimeoutMillis, TimeUnit.MILLISECONDS)
    .writeTimeout(ApiConfig.SocketTimeoutMillis, TimeUnit.MILLISECONDS)
    .build()
