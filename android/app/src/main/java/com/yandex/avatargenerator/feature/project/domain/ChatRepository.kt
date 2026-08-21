package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.GeneratedVersion
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
     * Отправляет промпт на генерацию (`POST /projects/{id}/generate`).
     *
     * Вызов долгий — модель отвечает десятками секунд. Успех означает, что
     * версия создана и **сервер сам дописал в историю пару «промпт → версия»**,
     * поэтому после него ленту надо перечитать через [getChat]: ни номера новой
     * версии, ни её `version_id` в ответе нет.
     *
     * При ошибке перечитывать нечего: запись в историю идёт после успешного
     * сохранения версии, так что «осиротевших» промптов в ней не появляется.
     *
     * @return ссылку на новую версию. `Result.failure` с
     *  [com.yandex.avatargenerator.core.domain.NetworkException]: 400 — промпт
     *  пуст или длиннее 1500 байт, 403 — чужой проект либо просроченный токен,
     *  `ClientError(422)` — модель вернула невалидный SVG (тот же промпт есть
     *  смысл повторить), `ServerError` — генератор временно недоступен.
     */
    suspend fun sendPrompt(projectId: String, prompt: String): Result<GeneratedVersion>
}
