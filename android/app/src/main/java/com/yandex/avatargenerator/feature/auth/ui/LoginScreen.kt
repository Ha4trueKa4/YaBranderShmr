package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.auth.ui.components.AuthBrandMark
import com.yandex.avatargenerator.feature.auth.ui.components.AuthPasswordVisibilityToggle
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.components.BrandTextLink
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран входа — Compose-версия макета Figma (node 3:393).
 *
 * Навигация отдана наружу коллбэками:
 * @param onSignedIn вход прошёл успешно — пускаем в приложение. Дёргается по
 *  событию из ViewModel, а не по нажатию кнопки.
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Одноразовые события навигации собираем, только пока экран не ниже STARTED —
    // иначе ответ, пришедший в фоне, дёрнул бы navigate на остановленном
    // NavController.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    LoginEvent.SignedIn -> onSignedIn()
                }
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
        onSubmit = viewModel::onSubmit,
        onForgotPasswordClick = onForgotPasswordClick,
        onNavigateToRegister = onNavigateToRegister,
        modifier = modifier,
    )
}

/**
 * Вёрстка экрана без ViewModel: состояние приходит параметром — так экран
 * рисуется в `@Preview`, не ходя в сеть.
 */
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.l),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBrandMark()

            Text(
                text = stringResource(R.string.auth_sign_in),
                style = AppTypography.heading2,
                modifier = Modifier.padding(top = AppSpacing.l)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.l),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
            ) {
                BrandTextField(
                    label = stringResource(R.string.auth_field_email),
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.auth_placeholder_email),
                    errorMessage = uiState.emailError?.text(),
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                BrandTextField(
                    label = stringResource(R.string.auth_field_password),
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(R.string.auth_placeholder_password),
                    errorMessage = uiState.passwordError?.text(),
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        AuthPasswordVisibilityToggle(
                            visible = uiState.isPasswordVisible,
                            onToggle = onPasswordVisibilityToggle
                        )
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.l),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ошибка не про конкретное поле (неверная пара логин/пароль,
                // сеть) — показываем над кнопкой.
                uiState.formError?.let { error ->
                    Text(
                        text = error.text(),
                        style = AppTypography.label,
                        color = AppColors.Error
                    )
                }

                BrandPrimaryButton(
                    text = stringResource(
                        if (uiState.isSubmitting) R.string.auth_signing_in else R.string.auth_sign_in,
                    ),
                    onClick = onSubmit,
                    fullWidth = true,
                    enabled = uiState.canSubmit,
                    leadingIcon = if (uiState.isSubmitting) {
                        {
                            CircularProgressIndicator(
                                // 18.dp / 2.dp — размеры индикатора внутри кнопки, точечные значения вне шкалы.
                                modifier = Modifier.size(18.dp),
                                color = AppColors.OnAccent,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        null
                    }
                )
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    style = AppTypography.label.copy(color = AppColors.Accent),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onForgotPasswordClick
                        )
                        .padding(AppSpacing.xxs)
                )
            }

            BrandTextLink(
                text = stringResource(R.string.auth_no_account),
                actionText = stringResource(R.string.auth_register),
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.l)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 393)
@Composable
private fun LoginScreenPreview() {
    AvatarGeneratorTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "hello@brandkit.ai",
                password = "secret123",
                formError = AuthFormError.InvalidCredentials,
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onSubmit = {},
            onForgotPasswordClick = {},
            onNavigateToRegister = {},
        )
    }
}
