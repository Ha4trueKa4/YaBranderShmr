package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel

/**
 * Единственный публичный вход в фичу онбординга — четыре слайда с иллюстрацией,
 * заголовком и общей кнопкой внизу (Figma `RTTuxBH32XkC0zryIFzuk1`, 2525:804…944).
 *
 * Навигацию фича на себя не берёт: куда идти дальше — решает `AppNavHost`.
 *
 * @param onFinish пользователь дошёл до конца; флаг «онбординг пройден» уже сохранён.
 */
@Composable
internal fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            OnboardingEffect.NavigateToHome -> onFinish()
        }
    }

    OnboardingContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
