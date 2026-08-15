package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.InMemoryAuthTokenStore
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import com.yandex.avatargenerator.feature.auth.fake.FakeAuthUserStore
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
import kotlinx.coroutines.flow.first
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
    private val userStore = FakeAuthUserStore()

    private fun repository(handler: MockRequestHandler): AuthRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return AuthRepositoryImpl(client, tokenStore, userStore)
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
    fun `login запоминает пользователя из ответа`() = runTest {
        val repository = repository { respond(AuthResponseJson, headers = jsonHeaders()) }

        repository.login("user@example.com", "secret")

        // Ручки /auth/me нет: ответ входа — единственный источник своего id.
        assertEquals(
            AuthUser(id = "u-1", email = "user@example.com", nickname = "John"),
            userStore.savedUser,
        )
        assertEquals(userStore.savedUser, repository.currentUser.first())
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
        assertEquals("u-1", userStore.savedUser?.id)
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

        assertTrue("получили $error", error is AuthException.FieldRejected)
        val rejected = error as AuthException.FieldRejected
        assertEquals(AuthException.Field.Password, rejected.field)
        assertEquals("слишком короткий", rejected.serverMessage)
    }

    @Test
    fun `422 разбирается так же, как 400`() = runTest {
        val repository = repository {
            respond(
                content = """{"code":"INVALID_NICKNAME","message":"короткий","field":"nickname"}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching {
            repository.register("user@example.com", "secret", "Jo")
        }.exceptionOrNull()

        assertTrue("получили $error", error is AuthException.FieldRejected)
        assertEquals(
            AuthException.Field.Nickname,
            (error as AuthException.FieldRejected).field,
        )
    }

    @Test
    fun `400 INVALID_EMAIL на login указывает на поле email`() = runTest {
        val repository = repository {
            respond(
                content = """{"code":"INVALID_EMAIL","message":"не email","field":"email"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }

        val error = runCatching { repository.login("не-email", "secret") }.exceptionOrNull()

        // Иначе пользователь увидел бы общее «что-то пошло не так».
        assertTrue("получили $error", error is AuthException.FieldRejected)
        assertEquals(
            AuthException.Field.Email,
            (error as AuthException.FieldRejected).field,
        )
        assertNull(tokenStore.currentToken())
    }

    @Test
    fun `чужой token_type роняет разбор и не сохраняет сессию`() = runTest {
        val repository = repository {
            respond(
                content = """
                    {
                      "access_token": "jwt-token",
                      "token_type": "MAC",
                      "user": { "id": "u-1", "email": "user@example.com", "nickname": "John" }
                    }
                """.trimIndent(),
                headers = jsonHeaders(),
            )
        }

        val error = runCatching { repository.login("user@example.com", "secret") }.exceptionOrNull()

        // Заголовок собирается строкой "Bearer $token": молчать о расхождении хуже.
        assertTrue("получили $error", error is NetworkException.Serialization)
        assertNull(tokenStore.currentToken())
        assertNull(userStore.savedUser)
    }

    @Test
    fun `регистр token_type значения не имеет`() = runTest {
        val repository = repository {
            respond(
                content = AuthResponseJson.replace("\"Bearer\"", "\"bearer\""),
                headers = jsonHeaders(),
            )
        }

        repository.login("user@example.com", "secret")

        assertEquals("jwt-token", tokenStore.currentToken())
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
    fun `logout забывает и токен, и пользователя`() = runTest {
        val repository = repository { respond(AuthResponseJson, headers = jsonHeaders()) }
        repository.login("user@example.com", "secret")

        repository.logout()

        assertNull(tokenStore.currentToken())
        assertNull(userStore.savedUser)
    }
}
