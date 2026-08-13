package com.yandex.avatargenerator.feature.auth.di

import com.yandex.avatargenerator.feature.auth.data.AuthRepositoryImpl
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import org.koin.dsl.module

/** Зависимости фичи авторизации. ViewModel экранов пока нет — сеть на них не заведена. */
val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(client = get(), tokenStore = get()) }
}
