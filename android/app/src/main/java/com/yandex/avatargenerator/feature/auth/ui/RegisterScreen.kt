package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.yandex.avatargenerator.feature.auth.ui.components.AuthFormLayout
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
 * Экран регистрации — Compose-версия макета Figma (node 3:355).
 *
 * Навигация отдана наружу коллбэками:
 * @param onRegistered аккаунт создан — пускаем в приложение. Дёргается по
 *  событию из ViewModel, а не по нажатию кнопки.
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Одноразовые события навигации собираем, только пока экран не ниже STARTED —
    // иначе ответ, пришедший в фоне, дёрнул бы navigate на остановленном
    // NavController.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    RegisterEvent.Registered -> onRegistered()
                }
            }
        }
    }

    RegisterContent(
        uiState = uiState,
        onNicknameChange = viewModel::onNicknameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
        onSubmit = viewModel::onSubmit,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

/**
 * Вёрстка экрана без ViewModel: состояние приходит параметром — так экран
 * рисуется в `@Preview`, не ходя в сеть.
 */
@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNicknameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(modifier = modifier) {
        AuthBrandMark()

        Text(
            text = stringResource(R.string.auth_create_account),
            style = AppTypography.heading2,
            modifier = Modifier.padding(top = AppSpacing.l)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
        ) {
            // В макете поле подписано «Full name», в API оно называется
            // nickname — расхождение осознанное, подпись не меняем.
            BrandTextField(
                label = stringResource(R.string.auth_field_full_name),
                value = uiState.nickname,
                onValueChange = onNicknameChange,
                placeholder = stringResource(R.string.auth_placeholder_full_name),
                errorMessage = uiState.nicknameError?.text(),
                height = 48.dp
            )
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
            // Ошибка не про конкретное поле (сеть, незнакомый код сервера) —
            // показываем над кнопкой.
            uiState.formError?.let { error ->
                Text(
                    text = error.text(),
                    style = AppTypography.label,
                    color = AppColors.Error
                )
            }

            BrandPrimaryButton(
                text = stringResource(
                    if (uiState.isSubmitting) {
                        R.string.auth_creating_account
                    } else {
                        R.string.auth_create_account
                    },
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
        }

        BrandTextLink(
            text = stringResource(R.string.auth_have_account),
            actionText = stringResource(R.string.auth_sign_in),
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.l)
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 393)
@Composable
private fun RegisterScreenPreview() {
    AvatarGeneratorTheme {
        RegisterContent(
            uiState = RegisterUiState(
                nickname = "John Doe",
                email = "hello@brandkit.ai",
                password = "12345",
                passwordError = AuthFormError.InvalidPassword,
            ),
            onNicknameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordVisibilityToggle = {},
            onSubmit = {},
            onNavigateToLogin = {},
        )
    }
}
