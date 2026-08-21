package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.project.data.ChatRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.ProjectRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.VersionsRepositoryImpl
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GeneratedVersion
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpResponseData
import io.ktor.http.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private const val LatestUrl = "https://s3.example/latest.svg?signature=abc"

private const val ProjectJson = """
{
  "id": "p-1",
  "author_id": "a-1",
  "name": "Orion Brand AI",
  "description": "",
  "is_public": true,
  "created_at": "2026-08-14T10:00:00+00:00",
  "url": "$LatestUrl"
}
"""

/**
 * Две пары «промпт → ответ». Формат `created_at` — тот, что реально отдаёт
 * Postgres (`jsonb_build_object('created_at', NOW())`): микросекунды и офсет
 * `+00:00`, а не `Z`. Порядок — добавления, старые сверху.
 */
private const val ChatJson = """
{
  "chat_history": [
    {"role": "user", "text": "Violet logo, please", "created_at": "2026-08-16T09:12:33.123456+00:00"},
    {"role": "assistant", "version_id": "v-2", "created_at": "2026-08-16T09:12:40.500000+00:00"},
    {"role": "user", "text": "Make it rounder", "created_at": "2026-08-16T10:00:00.000000+00:00"},
    {"role": "assistant", "version_id": "v-3", "created_at": "2026-08-16T10:00:20.000000+00:00"}
  ]
}
"""

/** `ORDER BY version_num DESC` — последняя версия первая. */
private const val VersionsJson = """
{
  "versions": [
    {"id": "v-3", "version_num": 3, "created_at": "2026-08-16T10:00:20+00:00"},
    {"id": "v-2", "version_num": 2, "created_at": "2026-08-16T09:12:40+00:00"},
    {"id": "v-1", "version_num": 1, "created_at": "2026-08-14T10:00:00+00:00"}
  ]
}
"""

private const val GeneratedUrl = "https://s3.example/generated.svg?signature=xyz"

/** Ответ генерации: кроме ссылки в нём нет ничего — ни version_id, ни номера. */
private const val GenerateJson = """
{"url": "$GeneratedUrl"}
"""

private typealias Responder = MockRequestHandleScope.() -> HttpResponseData

class ChatRepositoryImplTest {

    private val requestedPaths = CopyOnWriteArrayList<String>()

    private val requestedMethods = CopyOnWriteArrayList<HttpMethod>()

    /** Тела отправленных запросов — по ним проверяем контракт `POST /generate`. */
    private val sentBodies = CopyOnWriteArrayList<String>()

    /**
     * Экран собирается из трёх ручек, отправка промпта — четвёртая: мок
     * раскладывает ответы по путям. Проект и версии идут через боевые
     * репозитории, как и в графе Koin.
     */
    private fun repository(
        project: Responder = { respondJson(ProjectJson) },
        chat: Responder = { respondJson(ChatJson) },
        versions: Responder = { respondJson(VersionsJson) },
        generate: Responder = { respondJson(GenerateJson) },
    ): ChatRepositoryImpl {
        val client = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                requestedPaths += path
                requestedMethods += request.method
                (request.body as? TextContent)?.let { sentBodies += it.text }
                when {
                    path.endsWith("/chat") -> chat()
                    path.endsWith("/versions") -> versions()
                    path.endsWith("/generate") -> generate()
                    else -> project()
                }
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            // Плагин нужен здесь, потому что sendPrompt задаёт свой таймаут
            // через timeout { } — без плагина настройка просто не сработала бы.
            install(HttpTimeout)
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return ChatRepositoryImpl(
            client = client,
            projectRepository = ProjectRepositoryImpl(client),
            versionsRepository = VersionsRepositoryImpl(client),
        )
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        body,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    @Test
    fun `лента собирается из проекта, истории и версий`() = runTest {
        val chat = repository().getChat("p-1").getOrThrow()

        assertEquals("Orion Brand AI", chat.title)
        assertEquals(true, chat.isPublic)
        assertEquals(
            listOf(
                ChatMessage.System.ProjectCreated(id = "system-created"),
                ChatMessage.Outgoing(
                    id = "chat-0-user",
                    text = "Violet logo, please",
                    sentAt = Instant.parse("2026-08-16T09:12:33.123456Z"),
                ),
                // Превью нет: это не последняя версия, ссылку добудет PR 5.
                ChatMessage.Incoming(
                    id = "chat-1-assistant",
                    versionId = "v-2",
                    sentAt = Instant.parse("2026-08-16T09:12:40.500Z"),
                    previewUrl = null,
                ),
                ChatMessage.System.VersionSaved(
                    id = "chat-1-version",
                    version = "1",
                    versionId = "v-2",
                    createdAt = Instant.parse("2026-08-16T09:12:40.500Z"),
                ),
                ChatMessage.Outgoing(
                    id = "chat-2-user",
                    text = "Make it rounder",
                    sentAt = Instant.parse("2026-08-16T10:00:00Z"),
                ),
                ChatMessage.Incoming(
                    id = "chat-3-assistant",
                    versionId = "v-3",
                    sentAt = Instant.parse("2026-08-16T10:00:20Z"),
                    previewUrl = LatestUrl,
                ),
                ChatMessage.System.VersionSaved(
                    id = "chat-3-version",
                    version = "2",
                    versionId = "v-3",
                    createdAt = Instant.parse("2026-08-16T10:00:20Z"),
                ),
            ),
            chat.messages,
        )
    }

    @Test
    fun `разделители нумеруются по порядку ответов, а не по version_num`() = runTest {
        // В базе это версии 2 и 3 (первую заводит создание проекта), в ленте — 1 и 2.
        val versions = repository().getChat("p-1").getOrThrow().messages
            .filterIsInstance<ChatMessage.System.VersionSaved>()

        assertEquals(listOf("1", "2"), versions.map { it.version })
    }

    @Test
    fun `превью получает только последняя версия`() = runTest {
        val previews = repository().getChat("p-1").getOrThrow().messages
            .filterIsInstance<ChatMessage.Incoming>()

        assertEquals(listOf(null, LatestUrl), previews.map { it.previewUrl })
    }

    @Test
    fun `пустая история — это только системная пилюля`() = runTest {
        val repository = repository(chat = { respondJson("""{"chat_history": []}""") })

        val messages = repository.getChat("p-1").getOrThrow().messages

        assertEquals(listOf(ChatMessage.System.ProjectCreated(id = "system-created")), messages)
    }

    @Test
    fun `ответ без поля chat_history тоже даёт пустую ленту`() = runTest {
        val repository = repository(chat = { respondJson("{}") })

        assertEquals(1, repository.getChat("p-1").getOrThrow().messages.size)
    }

    @Test
    fun `403 на историю не роняет экран`() = runTest {
        // История приватна даже у публичного проекта: не-автору её не видно,
        // но сам проект должен открыться на чтение.
        val repository = repository(chat = { respondError(HttpStatusCode.Forbidden) })

        val chat = repository.getChat("p-1").getOrThrow()

        assertEquals("Orion Brand AI", chat.title)
        assertEquals(listOf(ChatMessage.System.ProjectCreated(id = "system-created")), chat.messages)
    }

    @Test
    fun `403 на версии оставляет ленту без превью`() = runTest {
        val repository = repository(versions = { respondError(HttpStatusCode.Forbidden) })

        val messages = repository.getChat("p-1").getOrThrow().messages

        assertEquals(7, messages.size)
        // Какая версия последняя — неизвестно, значит url проекта приписать некому.
        assertTrue(messages.filterIsInstance<ChatMessage.Incoming>().all { it.previewUrl == null })
    }

    @Test
    fun `кривые записи истории пропускаются, а не роняют разбор`() = runTest {
        val repository = repository(
            chat = {
                respondJson(
                    """
                    {"chat_history": [
                      {"role": "assistant", "created_at": "2026-08-16T09:00:00+00:00"},
                      {"role": "moderator", "text": "hi", "created_at": "2026-08-16T09:01:00+00:00"},
                      {"role": "user", "created_at": "2026-08-16T09:02:00+00:00"},
                      {"role": "user", "text": "ok", "created_at": "2026-08-16T09:03:00+00:00"}
                    ]}
                    """,
                )
            },
        )

        val messages = repository.getChat("p-1").getOrThrow().messages

        // Ответ без version_id, чужая роль и промпт без текста — мимо;
        // индекс в ключе остаётся исходным, поэтому уцелевшая запись — третья.
        assertEquals(
            listOf(
                ChatMessage.System.ProjectCreated(id = "system-created"),
                ChatMessage.Outgoing(
                    id = "chat-3-user",
                    text = "ok",
                    sentAt = Instant.parse("2026-08-16T09:03:00Z"),
                ),
            ),
            messages,
        )
    }

    @Test
    fun `кривой created_at оставляет сообщение без времени`() = runTest {
        val repository = repository(
            chat = { respondJson("""{"chat_history":[{"role":"user","text":"ok","created_at":"вчера"}]}""") },
        )

        val message = repository.getChat("p-1").getOrThrow().messages.last()

        assertNull((message as ChatMessage.Outgoing).sentAt)
    }

    @Test
    fun `запись без created_at тоже не роняет разбор`() = runTest {
        val repository = repository(
            chat = { respondJson("""{"chat_history":[{"role":"user","text":"ok"}]}""") },
        )

        val message = repository.getChat("p-1").getOrThrow().messages.last()

        assertNull((message as ChatMessage.Outgoing).sentAt)
    }

    @Test
    fun `падение запроса проекта — это ошибка экрана`() = runTest {
        // Без проекта нет даже заголовка, показывать нечего.
        val repository = repository(project = { respondError(HttpStatusCode.NotFound) })

        val error = repository.getChat("nope").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.NotFound)
    }

    @Test
    fun `история запрашивается по своему пути, сегменты кодируются`() = runTest {
        repository().getChat("a b")

        assertTrue(
            "запросы: $requestedPaths",
            requestedPaths.containsAll(
                listOf(
                    "/api/v1/projects/a%20b",
                    "/api/v1/projects/a%20b/chat",
                    "/api/v1/projects/a%20b/versions",
                ),
            ),
        )
    }

    @Test
    fun `удачная генерация отдаёт ссылку на новую версию`() = runTest {
        val generated = repository().sendPrompt("p-1", "Violet logo").getOrThrow()

        assertEquals(GeneratedVersion(previewUrl = GeneratedUrl), generated)
    }

    @Test
    fun `ответ 200 без url — не ошибка`() = runTest {
        // Версия создана, просто ссылку не прислали: возьмём её из ленты.
        val repository = repository(generate = { respondJson("{}") })

        assertEquals(
            GeneratedVersion(previewUrl = null),
            repository.sendPrompt("p-1", "Violet logo").getOrThrow(),
        )
    }

    @Test
    fun `промпт уходит POST-ом по своему пути, сегмент кодируется`() = runTest {
        repository().sendPrompt("a b", "Violet logo")

        assertEquals(listOf("/api/v1/projects/a%20b/generate"), requestedPaths.toList())
        assertEquals(listOf(HttpMethod.Post), requestedMethods.toList())
    }

    @Test
    fun `в теле уходит только промпт — модель бэкенд подставит сам`() = runTest {
        repository().sendPrompt("p-1", "Violet logo, please")

        val body = Json.parseToJsonElement(sentBodies.single()) as JsonObject
        assertEquals("Violet logo, please", body.getValue("prompt").jsonPrimitive.content)
        // explicitNulls = false: незаполненная модель в JSON не попадает,
        // иначе бэкенд получил бы null вместо своего дефолта.
        assertFalse("тело: $body", body.containsKey("model"))
    }

    @Test
    fun `400 на пустой или слишком длинный промпт — ClientError`() = runTest {
        val repository = repository(
            generate = { respondError(HttpStatusCode.BadRequest, """{"message":"Prompt too long"}""") },
        )

        val error = repository.sendPrompt("p-1", "x").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ClientError)
        assertEquals(400, (error as NetworkException.ClientError).code)
        assertEquals("Prompt too long", error.serverMessage)
    }

    @Test
    fun `403 — чужой проект или просроченный токен`() = runTest {
        val repository = repository(generate = { respondError(HttpStatusCode.Forbidden) })

        val error = repository.sendPrompt("p-1", "Violet logo").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }

    @Test
    fun `422 — модель вернула невалидный SVG`() = runTest {
        // Отдельно от 400: тот же промпт есть смысл повторить.
        val repository = repository(
            generate = {
                respondError(HttpStatusCode.UnprocessableEntity, """{"message":"Invalid SVG"}""")
            },
        )

        val error = repository.sendPrompt("p-1", "Violet logo").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ClientError)
        assertEquals(422, (error as NetworkException.ClientError).code)
    }

    @Test
    fun `503 — генератор временно недоступен`() = runTest {
        val repository = repository(
            generate = { respondError(HttpStatusCode.ServiceUnavailable) },
        )

        val error = repository.sendPrompt("p-1", "Violet logo").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ServerError)
    }

    @Test
    fun `неудачная генерация не перезапрашивает ленту`() = runTest {
        // Бэкенд пишет промпт в историю только после успешного сохранения
        // версии, так что после ошибки читать нечего.
        val repository = repository(generate = { respondError(HttpStatusCode.ServiceUnavailable) })

        repository.sendPrompt("p-1", "Violet logo")

        assertEquals(listOf("/api/v1/projects/p-1/generate"), requestedPaths.toList())
    }
}
