package com.yandex.avatargenerator.core.network

import android.util.Log
import com.yandex.avatargenerator.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

private const val LOG_TAG = "Network"
private const val MaxRetries = 2

/**
 * Сборка единственного на приложение [HttpClient] — он дорогой, создаётся один
 * раз в [com.yandex.avatargenerator.core.di.networkModule].
 *
 * Движок поднимается над [okHttpClient], который передаётся тем же, что
 * получает Coil — пул соединений и таймауты общие.
 */
object HttpClientFactory {

    /**
     * @param okHttpClient общий с Coil клиент ([createSharedOkHttpClient]).
     * @param enableLogging в release выключено: в телах ответов токены.
     */
    fun create(
        tokenProvider: AuthTokenProvider,
        json: Json = ApiJson,
        okHttpClient: OkHttpClient = createSharedOkHttpClient(),
        baseUrl: String = ApiConfig.BaseUrl,
        enableLogging: Boolean = BuildConfig.NETWORK_LOGGING,
    ): HttpClient = HttpClient(OkHttp.create { preconfigured = okHttpClient }) {
        expectSuccess = true // не-2xx → ResponseException, её разбирает apiCall()

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = ApiConfig.RequestTimeoutMillis
            connectTimeoutMillis = ApiConfig.ConnectTimeoutMillis
            socketTimeoutMillis = ApiConfig.SocketTimeoutMillis
        }

        installApiRetry()

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }

        install(authHeaderPlugin(tokenProvider))

        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d(LOG_TAG, message)
                    }
                }
                level = LogLevel.BODY
            }
        }
    }
}

/**
 * Повторяем 5xx и обрыв связи, и только для безопасных методов ([isRetryable]).
 * Вынесено отдельно, чтобы тест проверял ту же политику, что уходит в прод.
 */
internal fun HttpClientConfig<*>.installApiRetry() {
    install(HttpRequestRetry) {
        retryIf(maxRetries = MaxRetries) { request, response ->
            request.method.isRetryable() && response.status.value in 500..599
        }
        retryOnExceptionIf(maxRetries = MaxRetries) { request, cause ->
            request.method.isRetryable() && cause.isConnectionFailure()
        }
        exponentialDelay()
    }
}

/** Подставляет `Authorization: Bearer`, если токен есть и запрос не задал заголовок сам. */
private fun authHeaderPlugin(tokenProvider: AuthTokenProvider) =
    createClientPlugin(name = "AuthHeaderPlugin") {
        onRequest { request, _ ->
            if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest
            val token = tokenProvider.currentToken() ?: return@onRequest
            request.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
