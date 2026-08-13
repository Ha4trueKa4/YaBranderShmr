package com.yandex.avatargenerator.core.di

import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.AuthTokenProvider
import com.yandex.avatargenerator.core.network.AuthTokenStore
import com.yandex.avatargenerator.core.network.HttpClientFactory
import com.yandex.avatargenerator.core.network.InMemoryAuthTokenStore
import com.yandex.avatargenerator.core.network.createSharedOkHttpClient
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.dsl.module

/**
 * Сетевой слой: один [HttpClient] на приложение. Репозитории фич инъектят
 * готовый клиент, свой не собирают.
 */
val networkModule = module {
    single<Json> { ApiJson }

    // Один объект под двумя типами: репозиторий авторизации пишет токен,
    // клиент только читает.
    single<AuthTokenStore> { InMemoryAuthTokenStore() }
    single<AuthTokenProvider> { get<AuthTokenStore>() }

    // Общий с Coil клиент — см. HttpClientFactory.
    single<OkHttpClient> { createSharedOkHttpClient() }

    single<HttpClient> {
        HttpClientFactory.create(
            tokenProvider = get(),
            json = get(),
            okHttpClient = get(),
        )
    }
}
