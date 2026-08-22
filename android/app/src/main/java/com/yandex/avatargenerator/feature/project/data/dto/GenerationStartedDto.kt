package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `POST /projects/{id}/generate` (202): генерация только поставлена в
 * очередь, результата в нём нет — за ним ходят в
 * `GET /projects/{id}/generate/{job_id}` ([GenerationStatusDto]).
 *
 * @property jobId идентификатор фоновой задачи. Nullable — `required` в
 *  контракте не проставлен; нехватку ловит репозиторий с осмысленной ошибкой,
 *  а не разбор JSON.
 */
@Serializable
data class GenerationStartedDto(
    @SerialName("job_id") val jobId: String? = null,
)
