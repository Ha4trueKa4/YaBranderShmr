package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /projects/{id}/generate/{job_id}` — статус фоновой генерации.
 * Запрос «подвисает» на сервере до 25 секунд и отдаёт одно из трёх:
 *
 * - `pending` — не успела, запрос надо повторить;
 * - `done` + [url] — версия готова;
 * - `error` + [message] — генерация упала.
 *
 * @property status строкой, а не enum: незнакомый статус надо уметь пережить
 *  повтором опроса, а `kotlinx.serialization` уронил бы разбор всего ответа.
 * @property url presigned-ссылка на SVG новой версии; живёт час.
 * @property message текст ошибки от сервера; `required` в контракте нет —
 *  подпись пузыря на этот случай берётся из `strings.xml`.
 */
@Serializable
data class GenerationStatusDto(
    @SerialName("status") val status: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("message") val message: String? = null,
)
