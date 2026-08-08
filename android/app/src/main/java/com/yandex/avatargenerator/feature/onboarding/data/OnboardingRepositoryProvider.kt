package com.yandex.avatargenerator.feature.onboarding.data

import android.content.Context
import com.yandex.avatargenerator.core.data.appPreferences
import com.yandex.avatargenerator.feature.onboarding.domain.OnboardingRepository

/**
 * Собирает репозиторий онбординга (пока без DI, как и остальные фичи в проекте).
 *
 * Берём [Context.getApplicationContext]: репозиторий переживает пересоздание
 * активити, держать ссылку на неё нельзя.
 */
internal fun onboardingRepository(context: Context): OnboardingRepository =
    OnboardingPreferencesRepositoryImpl(context.applicationContext.appPreferences)
