package com.yandex.avatargenerator.feature.create.data

import com.yandex.avatargenerator.feature.create.data.dto.UserProjectsResponseDto
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDescriptionDto
import com.yandex.avatargenerator.feature.project.data.toInstantOrNull

/**
 * DTO выше `data/` не поднимаются: маппинг живёт здесь.
 *
 * Порядок сервера сохраняем: бэкенд отдаёт проекты `ORDER BY created_at DESC`,
 * то есть свежие уже сверху, и пересортировка клиентом только разошлась бы с ним.
 */
internal fun UserProjectsResponseDto.toChats(): List<Chat> =
    projects.orEmpty().mapNotNull { it.toChatOrNull() }

/**
 * Проект → чат. Битый элемент пропускаем, а не роняем весь список: один
 * испорченный проект не должен оставлять пользователя с пустым экраном
 * (так же делает `ProfileMappers`).
 */
private fun ProjectDescriptionDto.toChatOrNull(): Chat? {
    val id = id ?: return null
    val name = name ?: return null
    return Chat(
        id = id,
        title = name,
        updatedAt = createdAt?.toInstantOrNull(),
        previewUrl = url?.takeIf { it.isNotBlank() },
    )
}
