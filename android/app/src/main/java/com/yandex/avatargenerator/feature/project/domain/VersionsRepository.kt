package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.ProjectVersion
import com.yandex.avatargenerator.feature.project.domain.model.VersionRestore

/**
 * История версий проекта: `GET /projects/{id}/versions`,
 * `GET /projects/{id}/versions/{versionId}` и
 * `POST /projects/{id}/versions/{versionId}/restore`.
 *
 * Отдельный репозиторий, а не методы в [ProjectRepository]: у версий свой
 * ресурс, свои DTO и своя политика доступа (история видна **только автору**,
 * даже если сам проект публичный).
 *
 * Методы возвращают [Result] и не бросают исключений (кроме отмены корутины):
 * при ошибке в [Result.failure] лежит доменная
 * [com.yandex.avatargenerator.core.domain.NetworkException]. Разбор — во ViewModel.
 */
interface VersionsRepository {

    /**
     * Список версий проекта. Порядок сохраняем таким, каким его отдал сервер
     * (`ORDER BY version_num DESC`, новые сверху) — сортировать заново на
     * клиенте незачем. Пагинации у ручки нет, приходят все версии сразу.
     *
     * Требует `Bearer`. Не автор → `Forbidden` даже у публичного проекта,
     * нет проекта → `NotFound`. Пустой список — валидный ответ: у проекта без
     * ни одной генерации истории нет.
     */
    suspend fun getVersions(projectId: String): Result<List<ProjectVersion>>

    /**
     * Ссылка на SVG конкретной версии. Это **presigned S3-URL** с TTL в час
     * (`presigned-url-ttl-seconds: 3600` в конфиге бэкенда): сохранять его
     * надолго нельзя, за свежим нужен повторный вызов.
     *
     * Версии нет (или она от другого проекта) → `NotFound`.
     */
    suspend fun getVersionUrl(projectId: String, versionId: String): Result<String>

    /**
     * Откат к версии [versionId]. В ответе — **новая** версия, созданная
     * откатом (см. [VersionRestore]): история после вызова становится длиннее
     * на одну запись, а не короче, поэтому список надо перезапросить.
     *
     * Повторов при 5xx у этого вызова быть не должно: ретраятся только `GET` и
     * `HEAD` (`core/network/HttpMethod.isRetryable`), иначе шлюз, отдавший
     * ошибку после успешной записи, породил бы два отката подряд.
     */
    suspend fun restoreVersion(projectId: String, versionId: String): Result<VersionRestore>
}
