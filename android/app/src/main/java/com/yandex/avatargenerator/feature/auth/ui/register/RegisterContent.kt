package com.yandex.avatargenerator.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError
import com.yandex.avatargenerator.feature.auth.ui.components.AuthBrandMark
import com.yandex.avatargenerator.feature.auth.ui.components.AuthFormLayout
import com.yandex.avatargenerator.feature.auth.ui.components.AuthPasswordVisibilityToggle
import com.yandex.avatargenerator.feature.auth.ui.text
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.components.BrandTextLink
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Вёрстка экрана регистрации без ViewModel: всё, что рисуется, приходит
 * в [state], всё, что делает пользователь, уходит одним [onIntent] — так экран
 * рисуется в `@Preview`, не ходя в сеть.
 */
@Composable
internal fun RegisterContent(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
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
                value = state.nickname,
                onValueChange = { onIntent(RegisterIntent.OnNicknameChange(it)) },
                placeholder = stringResource(R.string.auth_placeholder_full_name),
                errorMessage = state.nicknameError?.text(),
                height = 48.dp
            )
            BrandTextField(
                label = stringResource(R.string.auth_field_email),
                value = state.email,
                onValueChange = { onIntent(RegisterIntent.OnEmailChange(it)) },
                placeholder = stringResource(R.string.auth_placeholder_email),
                errorMessage = state.emailError?.text(),
                height = 48.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            BrandTextField(
                label = stringResource(R.string.auth_field_password),
                value = state.password,
                onValueChange = { onIntent(RegisterIntent.OnPasswordChange(it)) },
                placeholder = stringResource(R.string.auth_placeholder_password),
                errorMessage = state.passwordError?.text(),
                height = 48.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    AuthPasswordVisibilityToggle(
                        visible = state.isPasswordVisible,
                        onToggle = { onIntent(RegisterIntent.OnPasswordVisibilityToggle) }
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
            state.formError?.let { error ->
                Text(
                    text = error.text(),
                    style = AppTypography.label,
                    color = AppColors.Error
                )
            }

            BrandPrimaryButton(
                text = stringResource(
                    if (state.isSubmitting) {
                        R.string.auth_creating_account
                    } else {
                        R.string.auth_create_account
                    },
                ),
                onClick = { onIntent(RegisterIntent.OnSubmitClick) },
                fullWidth = true,
                enabled = state.canSubmit,
                leadingIcon = if (state.isSubmitting) {
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
            onClick = { onIntent(RegisterIntent.OnLoginClick) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.l)
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 393)
@Composable
private fun RegisterContentPreview() {
    AvatarGeneratorTheme {
        RegisterContent(
            state = RegisterState(
                nickname = "John Doe",
                email = "hello@brandkit.ai",
                password = "12345",
                passwordError = AuthFormError.InvalidPassword,
            ),
            onIntent = {},
        )
    }
}
