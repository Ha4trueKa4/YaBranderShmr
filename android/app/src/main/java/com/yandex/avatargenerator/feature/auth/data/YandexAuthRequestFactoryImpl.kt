package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.core.network.YandexOAuthConfig
import com.yandex.avatargenerator.feature.auth.domain.YandexAuthRequestFactory
import com.yandex.avatargenerator.feature.auth.domain.model.YandexAuthRequest
import java.security.SecureRandom

/**
 * Собирает [YandexAuthRequest] по [YandexOAuthConfig]. `state` — случайный на
 * каждый заход, чтобы чужой редирект нельзя было подсунуть.
 */
class YandexAuthRequestFactoryImpl : YandexAuthRequestFactory {

    override fun isConfigured(): Boolean = YandexOAuthConfig.IsConfigured

    override fun create(): YandexAuthRequest {
        val state = randomState()
        return YandexAuthRequest(
            authorizeUrl = YandexOAuthConfig.authorizeUrl(state),
            redirectUri = YandexOAuthConfig.RedirectUri,
            state = state,
        )
    }

    private fun randomState(): String {
        val bytes = ByteArray(StateBytes)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val StateBytes = 16
        val secureRandom = SecureRandom()
    }
}
