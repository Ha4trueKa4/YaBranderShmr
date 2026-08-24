package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
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
     * Ставит промпт в очередь на генерацию (`POST /projects/{id}/generate`).
     *
     * Отвечает сразу, за миллисекунды: сама генерация уходит в фон, результат
     * забирает [awaitGeneration]. К моменту успешного возврата сервер **уже
     * дописал промпт в историю чата** — поэтому «осиротевший» промпт без ответа
     * в ленте нормален, пока генерация идёт, и убирать его с экрана после
     * неудачи нельзя: при следующем открытии он всё равно придёт с сервера.
     *
     * Ретраев у вызова нет и быть не должно: повтор создал бы вторую задачу и
     * вторую пару сообщений в истории.
     *
     * @param model короткое имя модели генерации (см.
     *  [com.yandex.avatargenerator.feature.project.domain.model.GenerationModel]).
     *  `null` — поле не уедет в теле вовсе и модель подставит бэкенд.
     *
     * @return `job_id` фоновой задачи. `Result.failure` с
     *  [com.yandex.avatargenerator.core.domain.NetworkException]: 400 — промпт
     *  пуст или длиннее 1500 байт, 403 — чужой проект либо просроченный токен,
     *  404 — проекта нет, `Serialization` — сервер не прислал `job_id`. Только в
     *  этом случае промпт до сервера не дошёл.
     */
    suspend fun startGeneration(
        projectId: String,
        prompt: String,
        model: String? = null,
    ): Result<String>

    /**
     * Дожидается результата задачи [jobId] опросом статус-ручки
     * (`GET /projects/{id}/generate/{job_id}`, long polling до 25 секунд на
     * запрос) и возвращает исход — см. [GenerationOutcome].
     *
     * Вызов долгий: ждёт до общего дедлайна (минуты). Обрыв сети между опросами
     * ожидание не прерывает — задача на сервере от этого никуда не девается.
     *
     * Отдельно от [startGeneration], потому что экран должен показать скелетон
     * ответа сразу по `job_id`, не дожидаясь результата, и различить «промпт не
     * ушёл» (ошибка отправки — текст можно вернуть в поле ввода) от «промпт ушёл,
     * генерация провалилась» (текст уже на сервере, возвращать его нельзя).
     *
     * @return исход ожидания. `Result.failure` — о судьбе генерации ничего не
     *  известно и записи о ней на сервере нет: опрос сорвался (403,
     *  неразобранный ответ) либо задача потеряна, а ответа с её `job_id` в
     *  истории не нашлось. Перечитывать ленту в этом случае бессмысленно —
     *  экран показывает свой пузырь ошибки.
     */
    suspend fun awaitGeneration(projectId: String, jobId: String): Result<GenerationOutcome>

    /**
     * Просит сервер прервать задачу [jobId]
     * (`POST /projects/{id}/generate/{job_id}/cancel`).
     *
     * Отвечает сразу и **не дожидается** фактической остановки: бэкенд успевает
     * ответить `cancelling`, пока фоновая задача ещё не заметила запрос. Исход
     * по-прежнему приносит идущий [awaitGeneration] — он увидит `cancelled` и
     * вернёт [GenerationOutcome.Cancelled]. Так у экрана остаётся один источник
     * правды об исходе, а не два конкурирующих ответа на один скелетон.
     *
     * Отмена — это тоже завершение генерации: сервер дописывает в историю чата
     * запись со `status = cancelled` и тем же `job_id`, что у промпта.
     *
     * @return `Result.failure` с
     *  [com.yandex.avatargenerator.core.domain.NetworkException]: 403 — чужой
     *  проект, 404 — задачи уже нет (бэкенд перезапустился либо она давно
     *  завершилась). Отмена не состоялась, ожидание идёт дальше своим чередом.
     */
    suspend fun cancelGeneration(projectId: String, jobId: String): Result<Unit>
}
