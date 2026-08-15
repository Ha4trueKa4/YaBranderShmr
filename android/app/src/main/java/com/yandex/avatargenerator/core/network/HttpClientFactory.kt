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
import io.ktor.http.Url
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

        installAuthHeader(tokenProvider, apiHost = Url(baseUrl).host)

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

/**
 * Подставляет `Authorization: Bearer` — только на запросы к [apiHost] и только
 * если запрос не задал заголовок сам. Вынесено отдельно, чтобы тест проверял ту
 * же политику, что уходит в прод.
 *
 * Проверка хоста нужна, чтобы первый же абсолютный URL на чужой домен (S3, CDN,
 * внешняя аватарка) не увёз токен пользователя третьей стороне.
 *
 * [apiHost] берём из того же `baseUrl`, что приходит в [HttpClientFactory.create],
 * а не из [ApiConfig] напрямую: иначе клиент со своим базовым адресом (тесты,
 * стенд) молча перестанет получать заголовок.
 */
internal fun HttpClientConfig<*>.installAuthHeader(
    tokenProvider: AuthTokenProvider,
    apiHost: String,
) {
    install(
        createClientPlugin(name = "AuthHeaderPlugin") {
            onRequest { request, _ ->
                // Хост у относительного пути появляется только после того, как
                // defaultRequest подставит базовый URL: оба перехватчика сидят на
                // фазе Before, поэтому installAuthHeader обязан идти после
                // defaultRequest — иначе host пустой и заголовка не будет вообще.
                if (!request.url.host.equals(apiHost, ignoreCase = true)) return@onRequest
                if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest
                val token = tokenProvider.currentToken() ?: return@onRequest
                request.header(HttpHeaders.Authorization, "Bearer $token")
            }
        },
    )
}
