package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.data.dto.ModelsResponseDto
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel

/** Схема URI, в которой `/models` отдаёт `id`: `gpt://<каталог>/<модель>`. */
private const val UriPrefix = "gpt://"

/**
 * Справочник моделей в доменный вид.
 *
 * Записи без `id` отсеиваем, хотя бэкенд их и так не отдаёт: контракт этого не
 * обещает. Дедуп — по уже срезанному `id`: запись OpenAI бэкенд дописывает с
 * проверкой на дубль, но сверяет полные URI.
 */
internal fun ModelsResponseDto.toDomain(): List<GenerationModel> =
    data.orEmpty()
        .mapNotNull { dto ->
            val id = dto.id?.toGenerationId() ?: return@mapNotNull null
            GenerationModel(id = id, owner = dto.ownedBy?.takeIf { it.isNotBlank() })
        }
        .distinctBy { it.id }

/**
 * Короткое имя модели для `POST /generate`.
 *
 * Бэкенд ждёт имя **без** префикса и клеит его сам
 * (`LLMClientComponent.cpp:456-457`), а признак OpenAI ищет по голому
 * `gpt-5.6-luna` (`llm_models.hpp:11`) — так что полный URI ломает обе ветки:
 * яндексовая уедет с двойным префиксом, а OpenAI-модель вообще уйдёт в Yandex API.
 *
 * Правило одно на обе: срезать `gpt://` и первый сегмент после него. Строку без
 * префикса отдаём как есть — она уже короткая.
 *
 * @return имя модели; `null` — из строки не осталось ничего осмысленного.
 */
private fun String.toGenerationId(): String? {
    val id = if (startsWith(UriPrefix)) {
        removePrefix(UriPrefix).substringAfter('/', missingDelimiterValue = "")
    } else {
        this
    }
    return id.takeIf { it.isNotBlank() }
}
