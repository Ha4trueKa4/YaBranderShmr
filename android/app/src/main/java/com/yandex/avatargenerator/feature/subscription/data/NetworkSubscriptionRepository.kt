package com.yandex.avatargenerator.feature.subscription.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.subscription.data.dto.SubscriptionUserDto
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.encodeURLPathPart

/**
 * Подписки через API. Инъектит готовый [HttpClient] (свой не собирает), `Bearer`
 * подставляет клиент сам — все ручки требуют авторизации. У subscribe/unsubscribe
 * тело ответа (`MessageResponse`) не разбираем: важен только факт успеха.
 * Списочные ручки отдают массив пользователей — маппим в домен ([toDomain]).
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

    override suspend fun getFollowers(userId: String): List<SubscriptionUser> = apiCall {
        client.get("users/${userId.encodeURLPathPart()}/subscribers").body<List<SubscriptionUserDto>>()
    }.toDomain()

    override suspend fun getFollowing(userId: String): List<SubscriptionUser> = apiCall {
        client.get("users/${userId.encodeURLPathPart()}/subscriptions").body<List<SubscriptionUserDto>>()
    }.toDomain()

    private fun subscribePath(userId: String): String =
        "users/${userId.encodeURLPathPart()}/subscribe"
}
