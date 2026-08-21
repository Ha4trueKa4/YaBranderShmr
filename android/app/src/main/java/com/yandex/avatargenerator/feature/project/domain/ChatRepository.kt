package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat

/**
 * Источник диалога с ИИ по проекту. Интерфейс живёт в `domain/`, боевая
 * реализация — [com.yandex.avatargenerator.feature.project.data.ChatRepositoryImpl];
 * [com.yandex.avatargenerator.feature.project.data.MockChatRepository] остаётся
 * только для `@Preview`.
 *
 * Методы возвращают [Result] и не бросают исключений (кроме отмены корутины):
 * разбор успеха/ошибки — на стороне ViewModel.
 */
interface ChatRepository {

    /**
     * Загружает проект вместе с историей диалога (`GET /projects/{id}/chat`).
     *
     * История приватна: у не-автора сервер отдаёт 403 даже на публичный проект.
     * Это не ошибка экрана — лента в таком случае приходит пустой, а сам проект
     * открывается на чтение.
     *
     * @return диалог проекта; у нового проекта [ProjectChat.messages] содержит
     *  только системную пилюлю «проект создан». `Result.failure` — если не
     *  удалось загрузить сам проект: без него нет даже заголовка.
     */
    suspend fun getChat(projectId: String): Result<ProjectChat>

    /**
     * Отправляет сообщение пользователя.
     *
     * @return созданное сообщение — а не `Unit`: серверный `id` и время
     *  проставляет источник данных, и UI из-за этого менять не придётся.
     */
    suspend fun sendMessage(projectId: String, text: String): Result<ChatMessage>
}
