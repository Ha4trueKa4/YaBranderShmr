package com.yandex.avatargenerator.feature.auth.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.yandex.avatargenerator.feature.auth.ui.components.YandexAuthWebView
import com.yandex.avatargenerator.feature.auth.ui.text
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.components.BrandTextLink
import com.yandex.avatargenerator.ui.components.BrandTonalButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Вёрстка экрана входа без ViewModel: всё, что рисуется, приходит в [state],
 * всё, что делает пользователь, уходит одним [onIntent] — так экран рисуется
 * в `@Preview`, не ходя в сеть.
 */
@Composable
internal fun LoginContent(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(modifier = modifier) {
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
                value = state.email,
                onValueChange = { onIntent(LoginIntent.OnEmailChange(it)) },
                placeholder = stringResource(R.string.auth_placeholder_email),
                errorMessage = state.emailError?.text(),
                height = 48.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            BrandTextField(
                label = stringResource(R.string.auth_field_password),
                value = state.password,
                onValueChange = { onIntent(LoginIntent.OnPasswordChange(it)) },
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
                        onToggle = { onIntent(LoginIntent.OnPasswordVisibilityToggle) }
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
            state.formError?.let { error ->
                Text(
                    text = error.text(),
                    style = AppTypography.label,
                    color = AppColors.Error
                )
            }

            BrandPrimaryButton(
                text = stringResource(
                    if (state.isSubmitting) R.string.auth_signing_in else R.string.auth_sign_in,
                ),
                onClick = { onIntent(LoginIntent.OnSubmitClick) },
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

        // Кнопку показываем, только если задан client_id: иначе экран согласия
        // Yandex открылся бы с ошибкой «отсутствует client_id».
        if (state.isYandexAvailable) {
            LoginOrDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.l)
            )

            BrandTonalButton(
                text = stringResource(R.string.auth_sign_in_with_yandex),
                onClick = { onIntent(LoginIntent.OnYandexClick) },
                fullWidth = true,
                enabled = !state.isBusy,
                modifier = Modifier.padding(top = AppSpacing.m),
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_yandex),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        BrandTextLink(
            text = stringResource(R.string.auth_no_account),
            actionText = stringResource(R.string.auth_register),
            onClick = { onIntent(LoginIntent.OnRegisterClick) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.l)
        )
    }

    state.yandexAuth?.let { request ->
        YandexAuthWebView(
            request = request,
            onCodeReceived = { onIntent(LoginIntent.OnYandexCodeReceived(it)) },
            onFailed = { onIntent(LoginIntent.OnYandexAuthFailed) },
            onDismiss = { onIntent(LoginIntent.OnYandexAuthDismissed) },
        )
    }
}

/** Разделитель «или» между входом по паролю и входом через Yandex ID. */
@Composable
private fun LoginOrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Border)
        Text(
            text = stringResource(R.string.auth_or_divider),
            style = AppTypography.label,
            color = AppColors.TextMuted,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Border)
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 393)
@Composable
private fun LoginContentPreview() {
    AvatarGeneratorTheme {
        LoginContent(
            state = LoginState(
                email = "hello@brandkit.ai",
                password = "secret123",
                formError = AuthFormError.InvalidCredentials,
                isYandexAvailable = true,
            ),
            onIntent = {},
        )
    }
}
