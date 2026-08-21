package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `POST /projects/{id}/generate`. Кроме ссылки в нём нет ничего: ни
 * `version_id`, ни номера версии, ни текста от модели — за ними нужен
 * перезапрос ленты.
 *
 * @property url presigned-ссылка на SVG новой версии; живёт час. Nullable —
 *  контракт не обещает поле, а падать разбором из-за его отсутствия незачем.
 */
@Serializable
data class GenerateProjectResponseDto(
    @SerialName("url") val url: String? = null,
)
