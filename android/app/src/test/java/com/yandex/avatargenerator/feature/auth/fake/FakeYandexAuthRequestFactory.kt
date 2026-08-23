package com.yandex.avatargenerator.feature.auth.fake

import com.yandex.avatargenerator.feature.auth.domain.YandexAuthRequestFactory
import com.yandex.avatargenerator.feature.auth.domain.model.YandexAuthRequest

/**
 * Фабрика-подделка: отдаёт фиксированный [request] и считает вызовы. Сети и
 * `BuildConfig` в тестах ViewModel нет — URL согласия здесь ни на что не влияет.
 */
internal class FakeYandexAuthRequestFactory(
    val request: YandexAuthRequest = DefaultRequest,
    private val configured: Boolean = true,
) : YandexAuthRequestFactory {

    var createCallCount: Int = 0
        private set

    override fun isConfigured(): Boolean = configured

    override fun create(): YandexAuthRequest {
        createCallCount++
        return request
    }

    companion object {
        val DefaultRequest = YandexAuthRequest(
            authorizeUrl = "https://oauth.yandex.ru/authorize?state=state-1",
            redirectUri = "avatargenerator://yandex-auth",
            state = "state-1",
        )
    }
}
