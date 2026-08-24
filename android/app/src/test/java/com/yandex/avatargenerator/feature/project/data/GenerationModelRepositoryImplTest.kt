package com.yandex.avatargenerator.feature.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** Ответ стенда с ключом OpenAI: три яндексовых записи из allow-list плюс своя. */
private const val ModelsJson = """
{
  "object": "list",
  "data": [
    {"id": "gpt://openai/gpt-5.6-luna", "object": "model", "owned_by": "OpenAI"},
    {"id": "gpt://b1gttnvmtpd0tagdlb7o/deepseek-v4-flash/latest", "object": "model", "owned_by": "DeepSeek", "created": 0},
    {"id": "gpt://b1gttnvmtpd0tagdlb7o/gpt-oss-120b/latest", "object": "model", "owned_by": "OpenAI", "created": 0}
  ]
}
"""

private typealias Responder = MockRequestHandleScope.() -> HttpResponseData

internal class GenerationModelRepositoryImplTest {

    private val requestedPaths = CopyOnWriteArrayList<String>()
    private val requestedMethods = CopyOnWriteArrayList<HttpMethod>()

    @Test
    fun `справочник читается своей ручкой, без сегментов проекта`() = runTest {
        repository().getAvailableModels()

        assertEquals(listOf("/api/v1/models"), requestedPaths.toList())
        assertEquals(listOf(HttpMethod.Get), requestedMethods.toList())
    }

    @Test
    fun `ответ разбирается с уже срезанными префиксами`() = runTest {
        val models = repository().getAvailableModels().getOrThrow()

        assertEquals(
            listOf(
                GenerationModel(id = "gpt-5.6-luna", owner = "OpenAI"),
                GenerationModel(id = GenerationModel.DefaultId, owner = "DeepSeek"),
                GenerationModel(id = "gpt-oss-120b/latest", owner = "OpenAI"),
            ),
            models,
        )
    }

    @Test
    fun `octet-stream от бэкенда разбирается наравне с json`() = runTest {
        // `/models` — единственная ручка на `HttpHandlerBase`, и content type она
        // не выставляет: JSON приезжает под `application/octet-stream`.
        // `ContentNegotiation` такой ответ конвертеру не отдаёт вовсе, поэтому
        // тело разбирает сам репозиторий.
        val repository = repository(
            models = { respond(ModelsJson, headers = headersOf(HttpHeaders.ContentType, "application/octet-stream")) },
        )

        val models = repository.getAvailableModels().getOrThrow()

        assertEquals(3, models.size)
    }

    @Test
    fun `5xx — failure, а не пустой список`() = runTest {
        // Разница важна экрану: пустой список он показал бы как «выбирать нечего»,
        // а ошибку — как повод предложить ретрай.
        val repository = repository(models = { respondError(HttpStatusCode.BadGateway) })

        val error = repository.getAvailableModels().exceptionOrNull()

        assertTrue("получили $error", error is NetworkException.ServerError)
    }

    @Test
    fun `пустое хранилище даёт дефолт бэкенда`() = runTest {
        assertEquals(GenerationModel.DefaultId, repository().getSelectedModelId())
    }

    @Test
    fun `выбор записывается и читается обратно`() = runTest {
        val repository = repository()

        repository.selectModel("gpt-5.6-luna")

        assertEquals("gpt-5.6-luna", repository.getSelectedModelId())
    }

    @Test
    fun `выбор живёт в общем хранилище — его видит и другой экземпляр`() = runTest {
        // Ключ один на приложение: выбор глобальный, не привязанный к проекту.
        val dataStore = InMemoryPreferencesDataStore()
        repository(dataStore = dataStore).selectModel("gpt-oss-120b/latest")

        assertEquals("gpt-oss-120b/latest", repository(dataStore = dataStore).getSelectedModelId())
    }

    private fun repository(
        models: Responder = { respondJson(ModelsJson) },
        dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore(),
    ): GenerationModelRepositoryImpl {
        val client = HttpClient(
            MockEngine { request ->
                requestedPaths += request.url.encodedPath
                requestedMethods += request.method
                models()
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            defaultRequest {
                url("http://localhost:8080/api/v1/")
                contentType(ContentType.Application.Json)
            }
        }
        return GenerationModelRepositoryImpl(client = client, dataStore = dataStore)
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        body,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}

/**
 * `DataStore` в памяти: настоящий пишет в файл и требует Android-контекста,
 * которого в юнит-тестах нет (та же причина, что у `FakeAuthUserStore`).
 * Проверяем здесь логику репозитория — ключ и дефолт, — а не сам DataStore.
 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences = transform(state.value).also { state.value = it }
}
