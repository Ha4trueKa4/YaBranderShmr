package com.yandex.avatargenerator.feature.auth

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.InMemoryAuthTokenStore
import com.yandex.avatargenerator.feature.auth.data.AuthRepositoryImpl
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val AuthResponseJson = """
{
  "access_token": "jwt-token",
  "token_type": "Bearer",
  "user": { "id": "u-1", "email": "user@example.com", "nickname": "John" }
}
"""

class AuthRepositoryImplTest {

    private val tokenStore = InMemoryAuthTokenStore()

    private fun repository(handler: MockRequestHandler): AuthRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return AuthRepositoryImpl(client, tokenStore)
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `login разбирает ответ и запоминает токен`() = runTest {
        val repository = repository { respond(AuthResponseJson, headers = jsonHeaders()) }

        val session = repository.login("user@example.com", "secret")

        assertEquals("jwt-token", session.accessToken)
        assertEquals("Bearer", session.tokenType)
        assertEquals("John", session.user.nickname)
        assertEquals("jwt-token", tokenStore.currentToken())
    }

    @Test
    fun `login уходит на нужный путь методом POST`() = runTest {
        var path: String? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            respond(AuthResponseJson, headers = jsonHeaders())
        }

        repository.login("user@example.com", "secret")

        assertEquals("/api/v1/auth/login", path)
    }

    @Test
    fun `401 на login превращается в InvalidCredentials и не трогает токен`() = runTest {
        val repository = repository { respondError(HttpStatusCode.Unauthorized) }

        val error = runCatching { repository.login("user@example.com", "wrong") }.exceptionOrNull()

        assertTrue(error is AuthException.InvalidCredentials)
        assertNull(tokenStore.currentToken())
    }

    @Test
    fun `register разбирает ответ 201 и запоминает токен`() = runTest {
        val repository = repository {
            respond(AuthResponseJson, status = HttpStatusCode.Created, headers = jsonHeaders())
        }

        val session = repository.register("user@example.com", "secret", "John")

        assertEquals("u-1", session.user.id)
        assertEquals("jwt-token", tokenStore.currentToken())
    }

    @Test
    fun `409 EMAIL_EXISTS указывает на поле email`() = runTest {
        val repository = repository {
            respond(
                content = """{"code":"EMAIL_EXISTS","message":"занят","field":"email"}""",
                status = HttpStatusCode.Conflict,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching {
            repository.register("user@example.com", "secret", "John")
        }.exceptionOrNull()

        assertTrue(error is AuthException.RegistrationConflict)
        val conflict = error as AuthException.RegistrationConflict
        assertEquals(AuthException.Field.Email, conflict.field)
        assertEquals("занят", conflict.serverMessage)
    }

    @Test
    fun `409 с незнакомым кодом не роняет разбор`() = runTest {
        val repository = repository {
            respond(
                content = """{"code":"SOMETHING_NEW"}""",
                status = HttpStatusCode.Conflict,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching {
            repository.register("user@example.com", "secret", "John")
        }.exceptionOrNull()

        assertEquals(
            AuthException.Field.Unknown,
            (error as AuthException.RegistrationConflict).field,
        )
    }

    @Test
    fun `400 INVALID_PASSWORD указывает на поле пароля`() = runTest {
        val repository = repository {
            respond(
                content = """{"code":"INVALID_PASSWORD","message":"слишком короткий","field":"password"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching {
            repository.register("user@example.com", "1", "John")
        }.exceptionOrNull()

        assertTrue("получили $error", error is AuthException.RegistrationRejected)
        val rejected = error as AuthException.RegistrationRejected
        assertEquals(AuthException.Field.Password, rejected.field)
        assertEquals("слишком короткий", rejected.serverMessage)
    }

    @Test
    fun `прочие 4xx на register остаются сетевой ошибкой`() = runTest {
        val repository = repository { respondError(HttpStatusCode.UnsupportedMediaType) }

        val error = runCatching {
            repository.register("user@example.com", "secret", "John")
        }.exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ClientError)
    }

    @Test
    fun `email приводится к нижнему регистру и обрезается`() = runTest {
        var body: String? = null
        val repository = repository { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(AuthResponseJson, headers = jsonHeaders())
        }

        repository.login("  User@Example.COM  ", "secret")

        assertTrue("ушло $body", body!!.contains("\"user@example.com\""))
    }

    @Test
    fun `никнейм обрезается по краям, пароль остаётся как есть`() = runTest {
        var body: String? = null
        val repository = repository { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(AuthResponseJson, status = HttpStatusCode.Created, headers = jsonHeaders())
        }

        repository.register("user@example.com", " secret ", "  John  ")

        assertTrue("ушло $body", body!!.contains("\"nickname\":\"John\""))
        assertTrue("ушло $body", body!!.contains("\"password\":\" secret \""))
    }

    @Test
    fun `logout забывает токен`() = runTest {
        val repository = repository { respond(AuthResponseJson, headers = jsonHeaders()) }
        repository.login("user@example.com", "secret")

        repository.logout()

        assertNull(tokenStore.currentToken())
    }
}
