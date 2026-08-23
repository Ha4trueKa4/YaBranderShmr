package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.YandexAuthRequest

/**
 * Собирает параметры очередного захода в Yandex ID (URL согласия + свежий
 * `state`). Интерфейс в `domain/`, чтобы ViewModel не знала про `BuildConfig` и
 * проверялась подделкой; реализация — в `data/`.
 */
interface YandexAuthRequestFactory {

    /** Настроен ли вход через Yandex ID (задан ли `client_id`). */
    fun isConfigured(): Boolean

    fun create(): YandexAuthRequest
}
