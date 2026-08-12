package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

/**
 * Единственный публичный вход в фичу онбординга.
 *
 * Навигацию фича на себя не берёт: экран сообщает о завершении колбэками,
 * а куда идти дальше — решает вызывающая сторона (`feature/appRouter`).
 *
 * @param onFinish пользователь нажал «Get Started»; флаг «онбординг пройден» уже сохранён
 * @param onSignIn пользователь нажал «Sign in»; флаг «онбординг пройден» уже сохранён
 */
@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingRoute(
        onFinish = onFinish,
        onSignIn = onSignIn,
        modifier = modifier,
        viewModel = koinViewModel(),
    )
}

@Composable
internal fun OnboardingRoute(
    onFinish: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onFinish, onSignIn) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToHome -> onFinish()
                OnboardingEffect.NavigateToSignIn -> onSignIn()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onPageChanged = viewModel::onPageChanged,
        onGetStartedClick = viewModel::onGetStartedClick,
        onSignInClick = viewModel::onSignInClick,
        modifier = modifier,
    )
}
