package com.yandex.avatargenerator.feature.create.domain.model

import java.time.Instant

/**
 * Чат пользователя с ИИ = один проект.
 *
 * Поля `previewUrl` пока нет: превью рисуем градиентом по [id] (см.
 * `feature/create/ui/components/ChatThumbnail.kt`). Добавим, когда бэкенд
 * начнёт отдавать картинки.
 *
 * @property id идентификатор; он же projectId при переходе на экран проекта.
 * @property title название, показывается в строке списка.
 * @property updatedAt время последнего изменения — из него считается подпись «15 min ago».
 */
data class Chat(
    val id: String,
    val title: String,
    val updatedAt: Instant,
)
