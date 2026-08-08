package com.yandex.avatargenerator.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow

/**
 * Хранит признак «онбординг уже показан».
 *
 * За интерфейсом, чтобы позже можно было переехать с DataStore на общий core/database
 * без правок ui-слоя.
 */
internal interface OnboardingRepository {

    val hasSeenOnboarding: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}
