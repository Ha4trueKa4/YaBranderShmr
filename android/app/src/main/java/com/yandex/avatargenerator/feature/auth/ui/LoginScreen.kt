package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
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
            LoginBrandMark()

            Text(
                text = "Sign in",
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
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "hello@brandkit.ai",
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                BrandTextField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Enter your password",
                    height = 48.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        LoginPasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible })
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
                BrandPrimaryButton(text = "Sign in", onClick = onSignInClick, fullWidth = true)
                Text(
                    text = "Forgot password?",
                    style = AppTypography.label.copy(color = AppColors.Accent),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onForgotPasswordClick
                        )
                        .padding(4.dp)
                )
            }

            LoginFooterSwitch(
                plainText = "No account?",
                actionText = "Register",
                onActionClick = onNavigateToRegister,
                modifier = Modifier.padding(top = AppSpacing.l)
            )
        }
    }
}

/** Небольшой квадратный значок бренда над заголовком формы (см. "small-logo" в макете Figma). */
@Composable
private fun LoginBrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(AppColors.Surface, RoundedCornerShape(AppShape.s))
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppShape.s)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.HighlightOff,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Переключатель видимости пароля — иконка "eye" из макета. */
@Composable
private fun LoginPasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier.size(20.dp)) {
        Icon(
            imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            contentDescription = null,
            tint = AppColors.TextMuted
        )
    }
}

/** Нижняя строка-переключатель на экран регистрации, с акцентным кликабельным словом. */
@Composable
private fun LoginFooterSwitch(
    plainText: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        append("$plainText ")
        withStyle(SpanStyle(color = AppColors.Accent, fontWeight = FontWeight.SemiBold)) {
            append(actionText)
        }
    }
    Text(
        text = annotated,
        color = AppColors.TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onActionClick
            )
    )
}

@Preview(showBackground = true, heightDp = 844, widthDp = 393)
@Composable
private fun LoginScreenPreview() {
    AvatarGeneratorTheme {
        LoginScreen()
    }
}
