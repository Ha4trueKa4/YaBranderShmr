package com.yandex.avatargenerator.feature.create.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.create.data.dto.UserProjectsResponseDto
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.flow.first

private const val UsersPath = "users"

/**
 * Список чатов через API. Своей ручки у списка проектов нет — берём `projects[]`
 * из `GET /users/{id}` текущего пользователя; своему профилю бэкенд отдаёт и
 * приватные проекты. Профиль разбирает тот же ответ отдельным запросом: экраны
 * независимы, дубль осознанный.
 *
 * Инъектит готовый [HttpClient] (свой не собирает), `Bearer` подставляет клиент
 * сам — без него в списке не было бы приватных проектов.
 */
class ChatsRepositoryImpl(
    private val client: HttpClient,
    private val authRepository: AuthRepository,
) : ChatsRepository {

    override suspend fun getChats(): Result<List<Chat>> = apiResult {
        // TODO: отдельное состояние экрана «войдите» вместо пустого списка.
        val userId = authRepository.currentUser.first()?.id ?: return@apiResult emptyList()
        apiCall {
            client.get("$UsersPath/${userId.encodeURLPathPart()}")
                .body<UserProjectsResponseDto>()
        }.toChats()
    }
}
