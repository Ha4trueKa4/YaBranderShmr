package com.yandex.avatargenerator.feature.auth.di

import com.yandex.avatargenerator.feature.auth.data.AuthRepositoryImpl
import com.yandex.avatargenerator.feature.auth.data.AuthUserPreferencesStore
import com.yandex.avatargenerator.feature.auth.data.AuthUserStore
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.auth.domain.CurrentUserUseCase
import com.yandex.avatargenerator.feature.auth.domain.IsAuthorizedUseCase
import com.yandex.avatargenerator.feature.auth.domain.LoginUseCase
import com.yandex.avatargenerator.feature.auth.domain.LogoutUseCase
import com.yandex.avatargenerator.feature.auth.domain.RegisterUseCase
import com.yandex.avatargenerator.feature.auth.ui.LoginViewModel
import com.yandex.avatargenerator.feature.auth.ui.RegisterViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Зависимости фичи авторизации: хранилище профиля, репозиторий, флаг сессии и
 * текущий пользователь для навигации, ViewModel обоих экранов.
 */
val authModule = module {
    // DataStore приходит из coreModule — свой файл настроек фича не заводит.
    single<AuthUserStore> { AuthUserPreferencesStore(get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(client = get(), tokenStore = get(), userStore = get())
    }
    factoryOf(::IsAuthorizedUseCase)
    factoryOf(::CurrentUserUseCase)
    factoryOf(::LoginUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::LogoutUseCase)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
