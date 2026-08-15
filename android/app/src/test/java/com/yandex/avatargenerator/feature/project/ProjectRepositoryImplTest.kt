package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.project.data.ProjectRepositoryImpl
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private const val ProjectJson = """
{
  "id": "p-1",
  "author_id": "u-1",
  "name": "Бренд кофейни",
  "description": "тёмные тона",
  "is_public": true,
  "created_at": "2026-08-13T12:00:00Z",
  "url": "https://s3.example/p-1.svg",
  "data": "<svg/>"
}
"""

class ProjectRepositoryImplTest {

    private fun repository(handler: MockRequestHandler): ProjectRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return ProjectRepositoryImpl(client)
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getProject разбирает ответ в доменную модель`() = runTest {
        val repository = repository { respond(ProjectJson, headers = jsonHeaders()) }

        val project = repository.getProject("p-1").getOrThrow()

        assertEquals("p-1", project.id)
        assertEquals("u-1", project.authorId)
        assertEquals("Бренд кофейни", project.name)
        assertTrue(project.isPublic)
        assertEquals(Instant.parse("2026-08-13T12:00:00Z"), project.createdAt)
        assertEquals("https://s3.example/p-1.svg", project.url)
        assertEquals("<svg/>", project.data)
    }

    @Test
    fun `getProject уходит на нужный путь методом GET`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            respond(ProjectJson, headers = jsonHeaders())
        }

        repository.getProject("p-1")

        assertEquals("/api/v1/projects/p-1", path)
        assertEquals(HttpMethod.Get, method)
    }

    @Test
    fun `getProject кодирует id в пути`() = runTest {
        var path: String? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            respond(ProjectJson, headers = jsonHeaders())
        }

        repository.getProject("a b")

        assertEquals("/api/v1/projects/a%20b", path)
    }

    @Test
    fun `created_at принимает офсет и дату без зоны, трактуя её как UTC`() = runTest {
        val withOffset = repository {
            respond("""{"id":"p","name":"X","created_at":"2026-08-14T10:00:00+03:00"}""", headers = jsonHeaders())
        }.getProject("p").getOrThrow()
        assertEquals(Instant.parse("2026-08-14T07:00:00Z"), withOffset.createdAt)

        val naive = repository {
            respond("""{"id":"p","name":"X","created_at":"2026-08-14T10:00:00"}""", headers = jsonHeaders())
        }.getProject("p").getOrThrow()
        assertEquals(Instant.parse("2026-08-14T10:00:00Z"), naive.createdAt)
    }

    @Test
    fun `отсутствие is_public трактуется как приватный`() = runTest {
        val repository = repository {
            respond("""{"id":"p-1","name":"X"}""", headers = jsonHeaders())
        }

        assertFalse(repository.getProject("p-1").getOrThrow().isPublic)
    }

    @Test
    fun `пустой id в ответе — это Serialization, а не тихий проект без id`() = runTest {
        val repository = repository {
            respond("""{"name":"X"}""", headers = jsonHeaders())
        }

        val error = repository.getProject("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Serialization)
    }

    @Test
    fun `404 на getProject превращается в NotFound`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        val error = repository.getProject("nope").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.NotFound)
    }

    @Test
    fun `updateProject шлёт PATCH с is_public в теле`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        var body: String? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            body = (request.body as TextContent).text
            respond("", headers = jsonHeaders())
        }

        repository.updateProject("p-1", isPublic = true)

        assertEquals("/api/v1/projects/p-1", path)
        assertEquals(HttpMethod.Patch, method)
        assertTrue("ушло $body", body!!.contains("\"is_public\":true"))
    }

    @Test
    fun `updateProject не шлёт поля, которые не трогаем`() = runTest {
        var body: String? = null
        val repository = repository { request ->
            body = (request.body as TextContent).text
            respond("", headers = jsonHeaders())
        }

        repository.updateProject("p-1", data = "<svg/>")

        assertFalse("is_public не должно уйти: $body", body!!.contains("is_public"))
        assertTrue("ушло $body", body!!.contains("\"data\":\"<svg/>\""))
    }

    @Test
    fun `deleteProject шлёт DELETE на нужный путь`() = runTest {
        var path: String? = null
        var method: HttpMethod? = null
        val repository = repository { request ->
            path = request.url.encodedPath
            method = request.method
            respond("", headers = jsonHeaders())
        }

        repository.deleteProject("p-1")

        assertEquals("/api/v1/projects/p-1", path)
        assertEquals(HttpMethod.Delete, method)
    }

    @Test
    fun `403 на deleteProject превращается в Forbidden`() = runTest {
        val repository = repository { respondError(HttpStatusCode.Forbidden) }

        val error = repository.deleteProject("p-1").exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.Forbidden)
    }
}
