package com.yandex.avatargenerator.feature.create.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import com.yandex.avatargenerator.feature.auth.fake.FakeAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Формат `created_at` — тот, что реально отдаёт бэкенд: офсет `+00:00`, а не `Z`.
 * Порядок — как в SQL: `ORDER BY created_at DESC`, свежие сверху. Поля профиля
 * в ответе есть, но списку чатов не нужны — их отбрасывает `ignoreUnknownKeys`.
 */
private const val UserProjectsJson = """
{
  "id": "user-1",
  "name": "Nick",
  "subscribers_count": 2,
  "projects": [
    {
      "id": "p-2",
      "name": "Chat 2",
      "is_public": false,
      "created_at": "2026-08-16T12:34:56+00:00",
      "url": "https://s3.example/p-2.svg?signature=abc"
    },
    {
      "id": "p-1",
      "name": "Chat 1",
      "is_public": true,
      "created_at": "2026-08-15T09:00:00+00:00"
    }
  ]
}
"""

class ChatsRepositoryImplTest {

    private val currentUser = AuthUser(id = "user-1", email = "user@example.com", nickname = "Nick")

    private fun repository(
        user: AuthUser? = currentUser,
        handler: MockRequestHandler,
    ): ChatsRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        val auth = FakeAuthRepository().apply { setCurrentUser(user) }
        return ChatsRepositoryImpl(client, auth)
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getChats уходит на профиль текущего пользователя методом GET`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            respond(UserProjectsJson, headers = jsonHeaders())
        }

        repository.getChats()

        assertEquals("/api/v1/users/user-1", path)
        assertEquals(HttpMethod.Get, method)
    }

    @Test
    fun `getChats разбирает проекты и сохраняет порядок сервера`() = runTest {
        val repository = repository { respond(UserProjectsJson, headers = jsonHeaders()) }

        val chats = repository.getChats().getOrThrow()

        assertEquals(listOf("p-2", "p-1"), chats.map { it.id })
        assertEquals(listOf("Chat 2", "Chat 1"), chats.map { it.title })
        assertEquals(Instant.parse("2026-08-16T12:34:56Z"), chats.first().updatedAt)
    }

    @Test
    fun `проект с сохранённой версией даёт превью, пустой — нет`() = runTest {
        val repository = repository { respond(UserProjectsJson, headers = jsonHeaders()) }

        val chats = repository.getChats().getOrThrow()

        assertEquals("https://s3.example/p-2.svg?signature=abc", chats.first().previewUrl)
        // Без url плитку рисует градиент по id — см. ChatThumbnail.
        assertNull(chats.last().previewUrl)
    }

    @Test
    fun `проект без id или name пропускается, остальной список остаётся`() = runTest {
        val json = """
            {"projects": [
              {"name": "Без id"},
              {"id": "p-1"},
              {"id": "p-2", "name": "Chat 2"}
            ]}
        """.trimIndent()
        val repository = repository { respond(json, headers = jsonHeaders()) }

        val chats = repository.getChats().getOrThrow()

        assertEquals(listOf("p-2"), chats.map { it.id })
    }

    @Test
    fun `нечитаемое время не роняет элемент — подписи просто нет`() = runTest {
        val json = """{"projects": [{"id": "p-1", "name": "Chat 1", "created_at": "вчера"}]}"""
        val repository = repository { respond(json, headers = jsonHeaders()) }

        val chats = repository.getChats().getOrThrow()

        assertEquals(1, chats.size)
        assertNull(chats.first().updatedAt)
    }

    @Test
    fun `пустой список проектов отдаётся как есть`() = runTest {
        val repository = repository { respond("""{"projects": []}""", headers = jsonHeaders()) }

        assertEquals(emptyList<String>(), repository.getChats().getOrThrow().map { it.id })
    }

    @Test
    fun `неизвестный пользователь не идёт в сеть и даёт пустой список`() = runTest {
        var requested = false
        val repository = repository(user = null) {
            requested = true
            respond(UserProjectsJson, headers = jsonHeaders())
        }

        assertEquals(emptyList<String>(), repository.getChats().getOrThrow().map { it.id })
        assertEquals(false, requested)
    }

    @Test
    fun `404 профиля приезжает как NotFound`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        val error = repository.getChats().exceptionOrNull()

        assertTrue(error is NetworkException.NotFound)
    }
}
