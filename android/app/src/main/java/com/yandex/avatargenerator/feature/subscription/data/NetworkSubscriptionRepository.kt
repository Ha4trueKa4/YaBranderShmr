package com.yandex.avatargenerator.feature.subscription.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.http.encodeURLPathPart

/**
 * Подписка через API. Инъектит готовый [HttpClient] (свой не собирает), `Bearer`
 * подставляет клиент сам — обе ручки требуют авторизации. Тело ответа
 * (`MessageResponse`) не разбираем: важен только факт успеха.
 */
class NetworkSubscriptionRepository(
    private val client: HttpClient,
) : SubscriptionRepository {

    override suspend fun subscribe(userId: String): Result<Unit> = apiResult {
        apiCall { client.post(subscribePath(userId)) }
    }

    override suspend fun unsubscribe(userId: String): Result<Unit> = apiResult {
        apiCall { client.delete(subscribePath(userId)) }
    }

    private fun subscribePath(userId: String): String =
        "users/${userId.encodeURLPathPart()}/subscribe"
}
