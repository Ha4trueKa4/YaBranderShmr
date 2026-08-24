package com.yandex.avatargenerator.feature.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.ModelsResponseDto
import com.yandex.avatargenerator.feature.project.domain.GenerationModelRepository
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val ModelsPath = "models"

/**
 * Модель генерации: справочник берём из API, выбор — из общего [DataStore]
 * (свой файл настроек фича не заводит, см. `core/data/AppPreferences.kt`).
 *
 * Инъектит готовый [HttpClient], свой не собирает. Ручка авторизации не требует,
 * но `Bearer` клиент подставит сам — бэкенду он не мешает.
 */
internal class GenerationModelRepositoryImpl(
    private val client: HttpClient,
    private val dataStore: DataStore<Preferences>,
) : GenerationModelRepository {

    /**
     * Тело разбираем сами, а не через `body<T>()`.
     *
     * `/models` — единственная ручка бэкенда на `HttpHandlerBase` вместо
     * `HttpHandlerJsonBase` (`get_models.hpp`), и `SetResponseContentType` в ней
     * не вызывается: JSON приезжает под `application/octet-stream`. Такой ответ
     * `ContentNegotiation` не отдаёт конвертеру вовсе — `body<T>()` падал бы
     * `NoTransformationFoundException`, то есть ошибкой разбора на исправном
     * ответе. Через [ApiJson] разбор идёт тем же самым `Json`, что и везде, и
     * переживёт починку заголовка на бэкенде.
     */
    override suspend fun getAvailableModels(): Result<List<GenerationModel>> = apiResult {
        apiCall {
            ApiJson.decodeFromString<ModelsResponseDto>(client.get(ModelsPath).bodyAsText())
        }.toDomain()
    }

    /** Настройки нечитаемы (IOException) — это не повод падать: отдаём дефолт. */
    override suspend fun getSelectedModelId(): String = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[SelectedModelKey]?.takeIf { it.isNotBlank() } }
        .first()
        ?: GenerationModel.DefaultId

    override suspend fun selectModel(id: String) {
        dataStore.edit { preferences -> preferences[SelectedModelKey] = id }
    }

    private companion object {
        val SelectedModelKey = stringPreferencesKey("selected_generation_model")
    }
}
