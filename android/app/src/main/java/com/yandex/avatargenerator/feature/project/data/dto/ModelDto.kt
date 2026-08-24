package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запись справочника моделей из `GET /models`.
 *
 * Оба поля nullable: `required` в swagger не проставлен, а сам ответ —
 * отфильтрованный проброс Yandex AI Studio плюс опциональная запись OpenAI, у
 * которой есть только `id` и `owned_by`. `object`/`created` не тянем — не нужны,
 * `ignoreUnknownKeys` их съест.
 *
 * @property id полный URI модели (`gpt://<провайдер>/<модель>`) — в `generate`
 *  как есть не годится, см. [com.yandex.avatargenerator.feature.project.data.toDomain].
 */
@Serializable
data class ModelDto(
    @SerialName("id") val id: String? = null,
    @SerialName("owned_by") val ownedBy: String? = null,
)
