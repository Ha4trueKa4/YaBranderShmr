package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.onboarding.domain.HasSeenOnboardingUseCase
import org.koin.compose.koinInject

/**
 * Показывали ли онбординг раньше — нужно навигации, чтобы выбрать стартовый экран.
 *
 * `null` означает «значение ещё читается из DataStore». Чтение асинхронное, и на
 * первом кадре ответа нет; вызывающий обязан дождаться не-null, иначе стартовый
 * экран выберется наугад и онбординг мигнёт уже прошедшему его пользователю.
 */
@Composable
fun rememberHasSeenOnboarding(): Boolean? {
    val hasSeenOnboarding = koinInject<HasSeenOnboardingUseCase>()
    return hasSeenOnboarding().collectAsStateWithLifecycle(initialValue = null).value
}
