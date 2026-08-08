package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.onboarding.data.onboardingRepository
import com.yandex.avatargenerator.feature.onboarding.domain.HasSeenOnboardingUseCase

/**
 * Показывали ли онбординг раньше — нужно навигации, чтобы выбрать стартовый экран.
 *
 * `null` означает «значение ещё читается из DataStore». Чтение асинхронное, и на
 * первом кадре ответа нет; вызывающий обязан дождаться не-null, иначе стартовый
 * экран выберется наугад и онбординг мигнёт уже прошедшему его пользователю.
 */
@Composable
fun rememberHasSeenOnboarding(): Boolean? {
    val context = LocalContext.current
    val hasSeenOnboarding = remember(context) {
        HasSeenOnboardingUseCase(onboardingRepository(context))
    }
    return hasSeenOnboarding().collectAsStateWithLifecycle(initialValue = null).value
}
