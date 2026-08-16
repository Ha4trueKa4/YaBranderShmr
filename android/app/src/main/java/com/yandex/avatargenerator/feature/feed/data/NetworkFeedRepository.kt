package com.yandex.avatargenerator.feature.feed.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.feature.feed.data.dto.FeedResponseDto
import com.yandex.avatargenerator.feature.feed.domain.model.FeedPage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val FeedPath = "feed"

/**
 * Лента через API (`GET /feed`). Инъектит готовый [HttpClient] (свой не
 * собирает), вызов заворачивает в [apiCall] — наружу летит только доменная
 * [com.yandex.avatargenerator.core.domain.NetworkException]. `Bearer`
 * подставляет клиент сам; для вкладки подписок ([FeedTab.SUBSCRIPTIONS])
 * токен обязателен, иначе бэкенд ответит `401`.
 */
class NetworkFeedRepository(
    private val client: HttpClient,
) : FeedRepository {

    override suspend fun getFeed(tab: FeedTab, cursor: String?, limit: Int): FeedPage = apiCall {
        client.get(FeedPath) {
            parameter("scope", tab.scope)
            parameter("limit", limit)
            if (cursor != null) parameter("cursor", cursor)
        }.body<FeedResponseDto>()
    }.toDomain()
}
