package com.yandex.avatargenerator.feature.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран входа — Compose-версия макета Figma (node 3:393).
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [LoginEffect], а не нажатием: переход на ленту случается по успешному ответу
 * сервера, когда токен уже сохранён.
 *
 * @param onSignedIn вход прошёл успешно — пускаем в приложение.
 * @param onNavigateToRegister переход на регистрацию.
 * @param onForgotPasswordClick восстановление пароля: ручки в API нет, поэтому
 *  по умолчанию ничего не делает (см. AUTH_PLAN.md, раздел «вне скоупа»).
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф — в
 * `feature/auth/di/AuthModule.kt`.
 */
@Composable
internal fun LoginScreen(
    onSignedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            LoginEffect.NavigateToApp -> onSignedIn()
            LoginEffect.NavigateToRegister -> onNavigateToRegister()
            LoginEffect.NavigateToPasswordRecovery -> onForgotPasswordClick()
        }
    }

    LoginContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
