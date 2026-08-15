package com.yandex.avatargenerator.feature.auth.di

import com.yandex.avatargenerator.feature.auth.data.AuthRepositoryImpl
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.auth.domain.IsAuthorizedUseCase
import com.yandex.avatargenerator.feature.auth.ui.LoginViewModel
import com.yandex.avatargenerator.feature.auth.ui.RegisterViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Зависимости фичи авторизации: репозиторий, флаг сессии для навигации и
 * ViewModel обоих экранов.
 */
val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(client = get(), tokenStore = get()) }
    factoryOf(::IsAuthorizedUseCase)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
