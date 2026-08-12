package com.yandex.avatargenerator.feature.onboarding.di

import com.yandex.avatargenerator.feature.onboarding.data.OnboardingPreferencesRepositoryImpl
import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.domain.HasSeenOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.domain.OnboardingRepository
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val onboardingModule = module {
    single<OnboardingRepository> { OnboardingPreferencesRepositoryImpl(get()) }
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::HasSeenOnboardingUseCase)
    viewModelOf(::OnboardingViewModel)
}
