package com.yandex.avatargenerator.feature.settings.di

import com.yandex.avatargenerator.feature.settings.data.SettingsRepositoryImpl
import com.yandex.avatargenerator.feature.settings.domain.SettingsRepository
import com.yandex.avatargenerator.feature.settings.ui.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Зависимости фичи настроек: репозиторий темы/языка (общий `DataStore` из
 * coreModule) и ViewModel экрана. `LogoutUseCase`/`CurrentUserUseCase` берутся
 * из authModule.
 */
val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
    viewModelOf(::SettingsViewModel)
}
