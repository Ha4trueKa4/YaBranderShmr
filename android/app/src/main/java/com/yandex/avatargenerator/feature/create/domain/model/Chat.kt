package com.yandex.avatargenerator.feature.create.domain.model

import java.time.Instant

/**
 * Чат пользователя с ИИ = один проект.
 *
 * @property id идентификатор; он же projectId при переходе на экран проекта.
 * @property title название, показывается в строке списка.
 * @property updatedAt время для подписи «15 min ago»; `null` — время не пришло
 *  или не разобралось, тогда подпись не рисуем. Заполняется из `created_at`:
 *  `updated_at` бэкенд в JSON не отдаёт, так что это время создания проекта.
 * @property previewUrl S3-ссылка на SVG последней версии; `null` у проекта без
 *  сохранённой версии — вместо картинки рисуем градиент по [id].
 */
data class Chat(
    val id: String,
    val title: String,
    val updatedAt: Instant?,
    val previewUrl: String? = null,
)
