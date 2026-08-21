package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Диалог с ИИ по одному проекту — содержимое экрана проекта.
 *
 * Название и видимость лежат здесь, а не запрашиваются экраном отдельно: лента
 * всё равно собирается из ответа `GET /projects/{id}`, и второй такой же запрос
 * из ViewModel был бы лишним.
 *
 * @property id идентификатор проекта (он же chatId в списке чатов).
 * @property title название проекта; показывается в шапке экрана — приходит из
 *  репозитория, а не хардкодится в UI.
 * @property isPublic видимость проекта.
 * @property messages лента сообщений, старые сверху; у нового проекта — только
 *  системная пилюля «проект создан».
 */
data class ProjectChat(
    val id: String,
    val title: String,
    val isPublic: Boolean,
    val messages: List<ChatMessage>,
)
