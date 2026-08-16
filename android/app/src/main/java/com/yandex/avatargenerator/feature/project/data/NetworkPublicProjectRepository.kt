package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLPathPart

/**
 * Витрина публичного проекта через API. Одна доменная модель собирается из двух
 * ручек: `GET /projects/{id}` (сам проект) и `GET /users/{author_id}` (автор и
 * его работы). `Bearer` подставляет клиент сам — для проекта он опционален
 * (публичный доступен без токена), для профиля автора нужен, чтобы бэкенд
 * заполнил `is_subscribed`.
 *
 * Инъектит готовый [HttpClient] (свой не собирает), каждый вызов — в [apiCall].
 * Наружу отдаёт [Result]: ошибку разбирает ViewModel.
 */
class NetworkPublicProjectRepository(
    private val client: HttpClient,
) : PublicProjectRepository {

    override suspend fun getPublicProject(projectId: String): Result<PublicProject> = apiResult {
        val project = apiCall {
            client.get("projects/${projectId.encodeURLPathPart()}").body<ProjectDto>()
        }
        val author = project.authorId?.let { fetchAuthorOrNull(it) }
        project.toPublicProject(author = author)
    }

    /**
     * Профиль автора для имени и подборки работ. Провал этой ручки не должен
     * ронять весь экран — проект показываем и без автора, поэтому [NetworkException]
     * глушим в `null` (отмену корутины [apiCall] пробрасывает, её не трогаем).
     */
    private suspend fun fetchAuthorOrNull(authorId: String): UserProfileResponseDto? =
        try {
            apiCall {
                client.get("users/${authorId.encodeURLPathPart()}").body<UserProfileResponseDto>()
            }
        } catch (network: NetworkException) {
            null
        }
}
