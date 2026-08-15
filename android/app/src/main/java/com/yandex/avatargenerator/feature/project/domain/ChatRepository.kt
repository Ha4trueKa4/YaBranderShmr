package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat

/**
 * Источник диалога с ИИ по проекту. Интерфейс живёт в `domain/`, реализации —
 * в `data/` (пока только [com.yandex.avatargenerator.feature.project.data.MockChatRepository]);
 * при готовности бэкенда добавится сетевая реализация без изменения UI.
 *
 * Методы возвращают [Result] и не бросают исключений (кроме отмены корутины):
 * разбор успеха/ошибки — на стороне ViewModel.
 */
interface ChatRepository {

    /** @return диалог проекта; у нового проекта [ProjectChat.messages] пустой. */
    suspend fun getChat(projectId: String): Result<ProjectChat>

    /**
     * Отправляет сообщение пользователя.
     *
     * @return созданное сообщение — а не `Unit`: серверный `id` и время
     *  проставляет источник данных, и UI из-за этого менять не придётся.
     */
    suspend fun sendMessage(projectId: String, text: String): Result<ChatMessage>
}
