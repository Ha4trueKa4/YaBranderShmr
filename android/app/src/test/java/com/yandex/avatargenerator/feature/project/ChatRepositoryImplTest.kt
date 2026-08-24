package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.project.data.ChatRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.GenerationPolling
import com.yandex.avatargenerator.feature.project.data.ProjectRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.VersionsRepositoryImpl
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
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

private const val JobId = "b3d9f2b0-6e3a-4b1a-9f0c-2b7b9a2d9c11"

/** Ответ 202: генерация только поставлена в очередь, результата в нём нет. */
private const val GenerateJson = """
{"job_id": "$JobId"}
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
    /**
     * Ответы статус-ручки по порядку опросов: первый вызов берёт первый элемент,
     * последний повторяется дальше — так задаётся «pending, pending, done».
     */
    private fun statuses(vararg responders: Responder): Responder {
        val calls = java.util.concurrent.atomic.AtomicInteger()
        return {
            responders[calls.getAndIncrement().coerceAtMost(responders.lastIndex)].invoke(this)
        }
    }

    private fun repository(
        project: Responder = { respondJson(ProjectJson) },
        chat: Responder = { respondJson(ChatJson) },
        versions: Responder = { respondJson(VersionsJson) },
        generate: Responder = { respondJson(GenerateJson) },
        status: Responder = { respondJson("""{"status":"done","url":"$GeneratedUrl"}""") },
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
                    path.contains("/generate/") -> status()
                    else -> project()
                }
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            // Плагин нужен здесь, потому что опрос статуса задаёт свой таймаут
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
            // Боевые тайминги — минуты; мок отвечает мгновенно, так что здесь
            // важны только пропорции, а не значения.
            polling = GenerationPolling(
                statusTimeoutMillis = 100,
                guardMillis = 1,
                deadlineMillis = 200,
            ),
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
    fun `старт генерации отдаёт job_id`() = runTest {
        assertEquals(JobId, repository().startGeneration("p-1", "Violet logo").getOrThrow())
    }

    @Test
    fun `202 без job_id — ошибка контракта, а не успех`() = runTest {
        // Иначе опрашивать было бы нечего, а экран решил бы, что всё идёт.
        val repository = repository(generate = { respondJson("{}") })

        val error = repository.startGeneration("p-1", "Violet logo").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `промпт уходит POST-ом по своему пути, сегмент кодируется`() = runTest {
        repository().startGeneration("a b", "Violet logo")

        assertEquals(listOf("/api/v1/projects/a%20b/generate"), requestedPaths.toList())
        assertEquals(listOf(HttpMethod.Post), requestedMethods.toList())
    }

    @Test
    fun `в теле уходит только промпт — модель бэкенд подставит сам`() = runTest {
        repository().startGeneration("p-1", "Violet logo, please")

        val body = Json.parseToJsonElement(sentBodies.single()) as JsonObject
        assertEquals("Violet logo, please", body.getValue("prompt").jsonPrimitive.content)
        // explicitNulls = false: незаполненная модель в JSON не попадает,
        // иначе бэкенд получил бы null вместо своего дефолта.
        assertFalse("тело: $body", body.containsKey("model"))
    }

    @Test
    fun `выбранная модель уходит в теле коротким именем`() = runTest {
        // Именно короткое: префикс `gpt://<каталог>/` бэкенд клеит сам, а по
        // полному URI он не опознал бы и OpenAI-модель.
        repository().startGeneration("p-1", "Violet logo", model = "gpt-5.6-luna")

        val body = Json.parseToJsonElement(sentBodies.single()) as JsonObject
        assertEquals("gpt-5.6-luna", body.getValue("model").jsonPrimitive.content)
    }

    @Test
    fun `400 на пустой или слишком длинный промпт — ClientError`() = runTest {
        val repository = repository(
            generate = { respondError(HttpStatusCode.BadRequest, """{"message":"Prompt too long"}""") },
        )

        val error = repository.startGeneration("p-1", "x").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ClientError)
        assertEquals(400, (error as NetworkException.ClientError).code)
        assertEquals("Prompt too long", error.serverMessage)
    }

    @Test
    fun `403 — чужой проект или просроченный токен`() = runTest {
        val repository = repository(generate = { respondError(HttpStatusCode.Forbidden) })

        val error = repository.startGeneration("p-1", "Violet logo").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }

    /*
     * Тесты опроса идут на runBlocking, а не runTest: виртуальные часы runTest
     * перематываются вперёд, как только на тест-диспетчере не осталось задач, —
     * а движок Ktor работает на своём. Общий дедлайн срабатывал бы мгновенно,
     * и любой сценарий заканчивался бы TimedOut. Ждать по-настоящему при этом
     * не приходится: тайминги подменены на миллисекунды (см. `repository`).
     */

    @Test
    fun `статус опрашивается по своему пути, job_id кодируется`() = runBlocking {
        repository().awaitGeneration("a b", "job 1")

        assertEquals(listOf("/api/v1/projects/a%20b/generate/job%201"), requestedPaths.toList())
        assertEquals(listOf(HttpMethod.Get), requestedMethods.toList())
    }

    @Test
    fun `done отдаёт ссылку на новую версию`() = runBlocking {
        val outcome = repository().awaitGeneration("p-1", JobId).getOrThrow()

        assertEquals(GenerationOutcome.Done(previewUrl = GeneratedUrl), outcome)
    }

    @Test
    fun `pending опрашивается ещё раз, пока не придёт done`() = runBlocking {
        // Long polling: сервер держит запрос 25 секунд и отвечает pending, если
        // за это время генерация не успела.
        val repository = repository(
            status = statuses(
                { respondJson("""{"status":"pending"}""") },
                { respondJson("""{"status":"pending"}""") },
                { respondJson("""{"status":"done","url":"$GeneratedUrl"}""") },
            ),
        )

        val outcome = repository.awaitGeneration("p-1", JobId).getOrThrow()

        assertEquals(GenerationOutcome.Done(previewUrl = GeneratedUrl), outcome)
        assertEquals(3, requestedPaths.size)
    }

    @Test
    fun `error отдаёт текст ошибки, а не Result-failure`() = runBlocking {
        // Провал генерации — это доведённое до конца ожидание: экран показывает
        // пузырь ошибки, а не «что-то пошло не так со связью».
        val repository = repository(
            status = { respondJson("""{"status":"error","message":"Invalid SVG"}""") },
        )

        assertEquals(
            GenerationOutcome.Failed(message = "Invalid SVG"),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `error без текста — тоже Failed, подпись подставит экран`() = runBlocking {
        val repository = repository(status = { respondJson("""{"status":"error"}""") })

        assertEquals(
            GenerationOutcome.Failed(message = null),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `обрыв связи между опросами не прерывает ожидание`() = runBlocking {
        // Сеть моргнула — задача на сервере от этого никуда не делась.
        val repository = repository(
            status = statuses(
                { throw ConnectException("network is down") },
                { respondJson("""{"status":"done","url":"$GeneratedUrl"}""") },
            ),
        )

        assertEquals(
            GenerationOutcome.Done(previewUrl = GeneratedUrl),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `403 на статус-ручке прерывает опрос ошибкой`() = runBlocking {
        // О судьбе генерации ничего не известно, повторять запрос бессмысленно.
        val repository = repository(status = { respondError(HttpStatusCode.Forbidden) })

        val error = repository.awaitGeneration("p-1", JobId).exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }

    @Test
    fun `404 ищет результат в истории и находит версию`() = runBlocking {
        // Задача живёт в памяти бэкенда: рестарт теряет её уже после того, как
        // генерация успела завершиться и попасть в историю.
        val repository = repository(
            status = { respondError(HttpStatusCode.NotFound) },
            chat = {
                respondJson(
                    """
                    {"chat_history": [
                      {"role": "user", "text": "Violet logo", "job_id": "$JobId"},
                      {"role": "assistant", "version_id": "v-9", "job_id": "$JobId"}
                    ]}
                    """,
                )
            },
        )

        // Ссылки в истории нет — её экран возьмёт из перечитанной ленты.
        assertEquals(
            GenerationOutcome.Done(previewUrl = null),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `404 находит в истории и провал генерации`() = runBlocking {
        val repository = repository(
            status = { respondError(HttpStatusCode.NotFound) },
            chat = {
                respondJson(
                    """
                    {"chat_history": [
                      {"role": "assistant", "error": "Invalid SVG", "job_id": "$JobId"}
                    ]}
                    """,
                )
            },
        )

        assertEquals(
            GenerationOutcome.Failed(message = "Invalid SVG"),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `404 без ответа в истории — ошибка, а не провал генерации`() = runBlocking {
        // Исход остался неизвестен: задача могла оборваться вместе с процессом,
        // а могла и досчитаться. Отдать Failed значило бы соврать экрану, что
        // на сервере лежит запись об ошибке, — он пошёл бы перечитывать ленту.
        val repository = repository(
            status = { respondError(HttpStatusCode.NotFound) },
            chat = { respondJson("""{"chat_history":[{"role":"user","text":"Violet logo"}]}""") },
        )

        val error = repository.awaitGeneration("p-1", JobId).exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.NotFound)
        // Пара попыток: запись в историю могла отстать от рестарта.
        assertEquals(2, requestedPaths.count { it.endsWith("/chat") })
    }

    @Test
    fun `вечный pending упирается в общий дедлайн`() = runBlocking {
        // runTest крутит виртуальное время — пять минут тест по-настоящему не ждёт.
        val repository = repository(status = { respondJson("""{"status":"pending"}""") })

        assertEquals(
            GenerationOutcome.TimedOut,
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }

    @Test
    fun `незнакомый статус не считается результатом`() = runBlocking {
        // Контракт мог расшириться — спрашиваем ещё раз вместо того, чтобы гадать.
        val repository = repository(
            status = statuses(
                { respondJson("""{"status":"queued"}""") },
                { respondJson("""{"status":"done","url":"$GeneratedUrl"}""") },
            ),
        )

        assertEquals(
            GenerationOutcome.Done(previewUrl = GeneratedUrl),
            repository.awaitGeneration("p-1", JobId).getOrThrow(),
        )
    }
}
