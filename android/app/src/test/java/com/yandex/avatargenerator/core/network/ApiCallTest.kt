package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.core.domain.NetworkException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Клиент собираем вручную, а не через [HttpClientFactory]: фабрика тянет
 * `BuildConfig` и `android.util.Log`, которых на JVM нет.
 */
class ApiCallTest {

    @Serializable
    private data class PayloadDto(val id: String)

    private fun clientReturning(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
        }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `успешный ответ разбирается в dto`() = runTest {
        val client = clientReturning {
            respond(content = """{"id":"42","unknownField":true}""", headers = jsonHeaders())
        }

        val payload = apiCall { client.get("payload").body<PayloadDto>() }

        assertEquals("42", payload.id)
    }

    @Test
    fun `401 превращается в Unauthorized с сообщением сервера`() = runTest {
        val client = clientReturning {
            respond(
                content = """{"message":"token expired"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching { apiCall { client.get("payload").body<PayloadDto>() } }.exceptionOrNull()

        assertTrue(error is NetworkException.Unauthorized)
        assertEquals("token expired", (error as NetworkException.Unauthorized).serverMessage)
    }

    @Test
    fun `5xx превращается в ServerError с кодом`() = runTest {
        val client = clientReturning { respondError(HttpStatusCode.BadGateway) }

        val error = runCatching { apiCall { client.get("payload").body<PayloadDto>() } }.exceptionOrNull()

        assertTrue(error is NetworkException.ServerError)
        assertEquals(502, (error as NetworkException.ServerError).code)
    }

    @Test
    fun `битое тело ответа превращается в Serialization`() = runTest {
        val client = clientReturning {
            respond(content = """{"wrong":"shape"}""", headers = jsonHeaders())
        }

        val error = runCatching { apiCall { client.get("payload").body<PayloadDto>() } }.exceptionOrNull()

        assertTrue(error is NetworkException.Serialization)
    }

    @Test
    fun `обрыв связи превращается в NoConnection`() = runTest {
        val error = runCatching {
            apiCall { throw UnknownHostException("api.example.com") }
        }.exceptionOrNull()

        assertTrue(error is NetworkException.NoConnection)
    }

    @Test
    fun `ошибка TLS не выдаётся за отсутствие сети`() = runTest {
        val error = runCatching {
            apiCall { throw SSLHandshakeException("certificate expired") }
        }.exceptionOrNull()

        assertTrue(error is NetworkException.Unknown)
    }

    @Test
    fun `HTTP-ошибка сохраняет исходное исключение в cause`() = runTest {
        val client = clientReturning { respondError(HttpStatusCode.BadRequest) }

        val error = runCatching { apiCall { client.get("payload").body<PayloadDto>() } }.exceptionOrNull()

        assertTrue(error is NetworkException.ClientError)
        assertTrue(error?.cause is ResponseException)
    }
}
