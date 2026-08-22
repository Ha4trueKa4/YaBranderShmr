package com.yandex.avatargenerator.feature.auth.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран регистрации — Compose-версия макета Figma (node 3:355).
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [RegisterEffect], а не нажатием: переход на ленту случается по успешному
 * ответу сервера, когда токен уже сохранён.
 *
 * @param onRegistered аккаунт создан — пускаем в приложение.
 * @param onNavigateToLogin переход на экран входа.
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф — в
 * `feature/auth/di/AuthModule.kt`.
 */
@Composable
internal fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            RegisterEffect.NavigateToApp -> onRegistered()
            RegisterEffect.NavigateToLogin -> onNavigateToLogin()
        }
    }

    RegisterContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
