package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.installApiRetry
import com.yandex.avatargenerator.feature.project.data.VersionsRepositoryImpl
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
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Формат `created_at` — тот, что реально отдаёт бэкенд:
 * `Timestring(..., "UTC", "%Y-%m-%dT%H:%M:%S%Ez")`, то есть офсет `+00:00`,
 * а не `Z`. Порядок — как в SQL: `ORDER BY version_num DESC`, новые сверху.
 */
private const val VersionsJson = """
{
  "versions": [
    {"id": "v-2", "version_num": 2, "created_at": "2026-08-16T12:34:56+00:00"},
    {"id": "v-1", "version_num": 1, "created_at": "2026-08-15T09:00:00+00:00"}
  ]
}
"""

private const val VersionUrlJson = """
{"version_id": "v-2", "url": "https://s3.example/v-2.svg?signature=abc"}
"""

class VersionsRepositoryImplTest {

    private fun repository(handler: MockRequestHandler): VersionsRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return VersionsRepositoryImpl(client)
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getVersions уходит на нужный путь методом GET`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            respond(VersionsJson, headers = jsonHeaders())
        }

        repository.getVersions("p-1")

        assertEquals("/api/v1/projects/p-1/versions", path)
        assertEquals(HttpMethod.Get, method)
    }

    @Test
    fun `getVersions разбирает список и сохраняет порядок сервера`() = runTest {
        val repository = repository { respond(VersionsJson, headers = jsonHeaders()) }

        val versions = repository.getVersions("p-1").getOrThrow()

        // Сервер отдаёт version_num DESC — новые сверху; заново не сортируем.
        assertEquals(listOf("v-2", "v-1"), versions.map { it.id })
        assertEquals(listOf(2, 1), versions.map { it.number })
        assertEquals(Instant.parse("2026-08-16T12:34:56Z"), versions.first().createdAt)
    }

    @Test
    fun `пустая история — это пустой список, а не ошибка`() = runTest {
        val repository = repository {
            respond("""{"versions": []}""", headers = jsonHeaders())
        }

        assertEquals(emptyList<Any>(), repository.getVersions("p-1").getOrThrow())
    }

    @Test
    fun `ответ без поля versions тоже даёт пустой список`() = runTest {
        val repository = repository { respond("{}", headers = jsonHeaders()) }

        assertEquals(emptyList<Any>(), repository.getVersions("p-1").getOrThrow())
    }

    @Test
    fun `версия без version_num — это Serialization`() = runTest {
        val repository = repository {
            respond("""{"versions":[{"id":"v-1"}]}""", headers = jsonHeaders())
        }

        val error = repository.getVersions("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `версия без id — это Serialization`() = runTest {
        val repository = repository {
            respond("""{"versions":[{"version_num":1}]}""", headers = jsonHeaders())
        }

        val error = repository.getVersions("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `кривой created_at не роняет разбор версии`() = runTest {
        val repository = repository {
            respond(
                """{"versions":[{"id":"v-1","version_num":1,"created_at":"вчера"}]}""",
                headers = jsonHeaders(),
            )
        }

        assertNull(repository.getVersions("p-1").getOrThrow().single().createdAt)
    }

    @Test
    fun `403 на getVersions превращается в Forbidden`() = runTest {
        // История приватна даже у публичного проекта: её видит только автор.
        val repository = repository { respondError(HttpStatusCode.Forbidden) }

        val error = repository.getVersions("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }

    @Test
    fun `404 на getVersions превращается в NotFound`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        val error = repository.getVersions("nope").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.NotFound)
    }

    @Test
    fun `пустое тело ошибки не ломает разбор`() = runTest {
        // Хендлеры версий отвечают на ошибку голым статусом, без {"message"}.
        val repository = repository { respond("", status = HttpStatusCode.Forbidden) }

        val error = repository.getVersions("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
        assertNull((error as NetworkException.Forbidden).serverMessage)
    }

    @Test
    fun `getVersionUrl уходит на путь версии и отдаёт ссылку`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            respond(VersionUrlJson, headers = jsonHeaders())
        }

        val url = repository.getVersionUrl("p-1", "v-2").getOrThrow()

        assertEquals("/api/v1/projects/p-1/versions/v-2", path)
        assertEquals(HttpMethod.Get, method)
        assertEquals("https://s3.example/v-2.svg?signature=abc", url)
    }

    @Test
    fun `ответ getVersionUrl без url — это Serialization`() = runTest {
        val repository = repository {
            respond("""{"version_id":"v-2"}""", headers = jsonHeaders())
        }

        val error = repository.getVersionUrl("p-1", "v-2").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `404 на getVersionUrl превращается в NotFound`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        val error = repository.getVersionUrl("p-1", "nope").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.NotFound)
    }

    @Test
    fun `restoreVersion шлёт POST с пустым объектом и разбирает ответ`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        var body: String? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            body = (request.body as TextContent).text
            respond(VersionUrlJson, headers = jsonHeaders())
        }

        val restore = repository.restoreVersion("p-1", "v-1").getOrThrow()

        assertEquals("/api/v1/projects/p-1/versions/v-1/restore", path)
        assertEquals(HttpMethod.Post, method)
        // Хендлер тело не читает, но парсит его как JSON: пустое не подойдёт.
        assertEquals("{}", body)
        // version_id в ответе — id НОВОЙ версии, а не той, к которой откатились.
        assertEquals("v-2", restore.id)
        assertEquals("https://s3.example/v-2.svg?signature=abc", restore.url)
    }

    @Test
    fun `ответ restoreVersion без version_id — это Serialization`() = runTest {
        val repository = repository {
            respond("""{"url":"https://s3.example/v.svg"}""", headers = jsonHeaders())
        }

        val error = repository.restoreVersion("p-1", "v-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `403 на restoreVersion превращается в Forbidden`() = runTest {
        val repository = repository { respondError(HttpStatusCode.Forbidden) }

        val error = repository.restoreVersion("p-1", "v-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }

    @Test
    fun `пути кодируются посегментно`() = runTest {
        var path: String? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            respond(VersionUrlJson, headers = jsonHeaders())
        }

        repository.getVersionUrl("a b", "c/d")

        assertEquals("/api/v1/projects/a%20b/versions/c%2Fd", path)
    }

    @Test
    fun `restoreVersion не повторяется при 5xx`() = runTest {
        // Повтор после ошибки шлюза создал бы второй откат — то есть лишнюю
        // версию. Политику ретраев берём боевую, а не пишем свою.
        val attempts = AtomicInteger(0)
        val client = HttpClient(
            MockEngine {
                attempts.incrementAndGet()
                respondError(HttpStatusCode.BadGateway)
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            installApiRetry()
            defaultRequest { url("http://localhost:8080/api/v1/") }
        }

        val error = VersionsRepositoryImpl(client).restoreVersion("p-1", "v-1").exceptionOrNull()

        assertEquals(1, attempts.get())
        assertTrue("получили $error", error is NetworkException.ServerError)
    }
}
