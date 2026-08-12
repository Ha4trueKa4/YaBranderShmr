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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

/**
 * Экран входа — Compose-версия макета Figma (node 3:393). Пока чистый UI:
 * поля хранят состояние локально, колбэки — заглушки без реальной авторизации.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                    value = email,
                    onValueChange = { email = it },
                    placeholder = stringResource(R.string.auth_placeholder_email),
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                BrandTextField(
                    label = stringResource(R.string.auth_field_password),
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(R.string.auth_placeholder_password),
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AuthPasswordVisibilityToggle(
                            visible = passwordVisible,
                            onToggle = { passwordVisible = !passwordVisible }
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
                BrandPrimaryButton(
                    text = stringResource(R.string.auth_sign_in),
                    onClick = onSignInClick,
                    fullWidth = true
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
        LoginScreen()
    }
}
