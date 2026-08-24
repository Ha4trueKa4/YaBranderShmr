package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.Project
import com.yandex.avatargenerator.feature.project.domain.model.ProjectCreation

/**
 * Создание, чтение и изменение проекта: `POST /projects` плюс
 * `GET`/`PATCH`/`DELETE /projects/{id}`.
 *
 * Методы возвращают [Result] и не бросают исключений (кроме отмены корутины):
 * при ошибке в [Result.failure] лежит доменная
 * [com.yandex.avatargenerator.core.domain.NetworkException]. Разбор — во ViewModel.
 */
interface ProjectRepository {

    /**
     * `POST /projects` — создаёт проект и сразу запускает его первичную
     * генерацию: ответ `202`, в нём весь `Project` и `job_id` фоновой задачи
     * (см. [ProjectCreation]). Ждут её тем же [ChatRepository.awaitGeneration],
     * что и правки в чате. Требует `Bearer`.
     *
     * Экран создания живёт в `feature/createproject`, но своего репозитория не
     * заводит: ответ — та же доменная модель, а DTO выше `data/` своей фичи не
     * поднимаются, поэтому метод здесь.
     *
     * @param prompt что генерировать; обязателен, до 5000 UTF-8 символов.
     *  Бэкенд пишет его в историю чата синхронно с созданием, поэтому первый же
     *  `getChat` на экране проекта его вернёт. Пустой промпт — `ClientError(400)`.
     * @param name название проекта; пустая строка допустима — бэкенд читает
     *  поле с дефолтом `""`.
     * @param description бриф бренда; тоже опционален, дефолт `""`.
     * @param model короткое имя модели генерации (см.
     *  [com.yandex.avatargenerator.feature.project.domain.model.GenerationModel]) —
     *  первичная генерация пойдёт на ней. `null` — поле не уедет в теле вовсе
     *  и модель подставит бэкенд.
     */
    suspend fun createProject(
        name: String,
        description: String,
        prompt: String,
        model: String? = null,
    ): Result<ProjectCreation>

    /**
     * `GET /projects/{id}`. Авторизация опциональна: публичный проект доступен
     * без токена. При ошибке в `failure` — `NetworkException` (`NotFound` для
     * 404, `Forbidden` для приватного чужого проекта).
     */
    suspend fun getProject(id: String): Result<Project>

    /**
     * `PATCH /projects/{id}` — частичное обновление. `null`-аргумент означает
     * «не трогать поле» (см. `explicitNulls = false` в `ApiJson`). Тела в ответе
     * нет. Требует `Bearer`; `Forbidden`, если не автор.
     */
    suspend fun updateProject(
        id: String,
        isPublic: Boolean? = null,
        data: String? = null,
    ): Result<Unit>

    /**
     * `DELETE /projects/{id}`. Тела в ответе нет. Требует `Bearer`; `Forbidden`
     * для чужого проекта, `NotFound`, если проекта нет.
     */
    suspend fun deleteProject(id: String): Result<Unit>
}
