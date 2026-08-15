package com.yandex.avatargenerator.core.network

import kotlinx.coroutines.CancellationException

/**
 * Сводит вызов (обычно [apiCall] плюс маппинг) в [Result], чтобы репозиторий
 * не бросал, а отдавал `Result` — разбор на стороне ViewModel.
 *
 * Отмену корутины ([CancellationException]) пробрасываем — её глотать нельзя.
 * `Error` (OOM и т.п.) не ловим осознанно: он должен долетать до крэш-репортера
 * как есть (та же логика, что в [apiCall]). Всё остальное — в [Result.failure];
 * после [apiCall] там лежит доменная
 * [com.yandex.avatargenerator.core.domain.NetworkException].
 *
 * ```
 * override suspend fun getProject(id: String): Result<Project> = apiResult {
 *     apiCall { client.get("projects/$id").body<ProjectDto>() }.toDomain()
 * }
 * ```
 */
inline fun <T> apiResult(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }
