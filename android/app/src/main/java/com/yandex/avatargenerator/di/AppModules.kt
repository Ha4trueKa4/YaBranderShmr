package com.yandex.avatargenerator.di

import com.yandex.avatargenerator.core.di.coreModule
import com.yandex.avatargenerator.feature.create.di.chatsModule
import com.yandex.avatargenerator.feature.feed.di.feedModule
import com.yandex.avatargenerator.feature.onboarding.di.onboardingModule
import com.yandex.avatargenerator.feature.profile.di.profileModule

/**
 * Полный граф зависимостей приложения: общий [coreModule] плюс модуль каждой
 * фичи. Подключается в [com.yandex.avatargenerator.AvatarGeneratorApplication].
 */
val appModules = listOf(
    coreModule,
    onboardingModule,
    feedModule,
    chatsModule,
    profileModule,
)
