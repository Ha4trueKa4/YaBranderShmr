package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Диалог с ИИ по одному проекту — содержимое экрана проекта.
 *
 * @property id идентификатор проекта (он же chatId в списке чатов).
 * @property title название проекта; показывается в шапке экрана — приходит из
 *  репозитория, а не хардкодится в UI.
 * @property messages лента сообщений, старые сверху; пустая у нового проекта.
 */
data class ProjectChat(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
)
