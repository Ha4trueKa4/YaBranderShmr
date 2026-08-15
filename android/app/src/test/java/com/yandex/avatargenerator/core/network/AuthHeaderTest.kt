package com.yandex.avatargenerator.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val BaseUrl = "https://api.example.com/api/v1/"
private const val ApiHost = "api.example.com"

/** Политика заголовка `Authorization` из [installAuthHeader] — ровно та, что уходит в прод. */
class AuthHeaderTest {

    /** Заголовок, с которым запрос реально дошёл до движка. */
    private var sentAuthorization: String? = null

    private fun tokenProvider(token: String?) = object : AuthTokenProvider {
        override suspend fun currentToken(): String? = token
    }

    private fun client(token: String?): HttpClient {
        val engine = MockEngine { request ->
            sentAuthorization = request.headers[HttpHeaders.Authorization]
            respondOk()
        }
        return HttpClient(engine) {
            // Порядок принципиален: defaultRequest подставляет базовый URL раньше,
            // иначе у относительного пути не будет хоста и заголовок пропадёт совсем.
            defaultRequest { url(BaseUrl) }
            installAuthHeader(tokenProvider(token), apiHost = ApiHost)
        }
    }

    @Test
    fun `относительный путь получает Bearer`() = runTest {
        client("jwt-token").get("auth/me")

        assertEquals("Bearer jwt-token", sentAuthorization)
    }

    @Test
    fun `абсолютный URL на чужой хост токен не получает`() = runTest {
        client("jwt-token").get("https://cdn.example.org/avatars/u-1.svg")

        assertNull(sentAuthorization)
    }

    @Test
    fun `без токена заголовка нет`() = runTest {
        client(null).get("auth/me")

        assertNull(sentAuthorization)
    }

    @Test
    fun `заголовок самого запроса не перетирается`() = runTest {
        client("jwt-token").get("auth/me") {
            header(HttpHeaders.Authorization, "Basic custom")
        }

        assertEquals("Basic custom", sentAuthorization)
    }
}
