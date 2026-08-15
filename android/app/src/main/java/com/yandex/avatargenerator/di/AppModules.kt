package com.yandex.avatargenerator.di

import com.yandex.avatargenerator.core.di.coreModule
import com.yandex.avatargenerator.core.di.networkModule
import com.yandex.avatargenerator.feature.auth.di.authModule
import com.yandex.avatargenerator.feature.create.di.chatsModule
import com.yandex.avatargenerator.feature.createproject.di.createProjectModule
import com.yandex.avatargenerator.feature.feed.di.feedModule
import com.yandex.avatargenerator.feature.onboarding.di.onboardingModule
import com.yandex.avatargenerator.feature.profile.di.profileModule

/**
 * Полный граф зависимостей приложения: общие [coreModule] и [networkModule]
 * плюс модуль каждой фичи. Подключается в
 * [com.yandex.avatargenerator.AvatarGeneratorApplication].
 */
val appModules = listOf(
    coreModule,
    networkModule,
    onboardingModule,
    authModule,
    feedModule,
    chatsModule,
    createProjectModule,
    profileModule,
)
