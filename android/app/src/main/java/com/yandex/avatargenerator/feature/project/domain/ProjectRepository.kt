package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.Project

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
     * `POST /projects` — создаёт пустой проект и отдаёт его целиком (в ответе
     * тот же `Project`, что и у [getProject], только без `url`: SVG появится
     * после первой генерации). Требует `Bearer`.
     *
     * Экран создания живёт в `feature/createproject`, но своего репозитория не
     * заводит: ответ — та же доменная модель, а DTO выше `data/` своей фичи не
     * поднимаются, поэтому метод здесь.
     *
     * @param description бриф бренда; пустая строка допустима — бэкенд читает
     *  поле с дефолтом `""`. Пустой [name] бэкенд отвергает как `ClientError(400)`.
     */
    suspend fun createProject(name: String, description: String): Result<Project>

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
