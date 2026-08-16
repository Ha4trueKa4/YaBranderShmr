package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Строка вида «обычный текст + акцентное слово-действие» — футер экранов входа
 * и регистрации, приглашение войти в онбординге.
 *
 * Кликается **целиком**, а не только акцентная часть: отдельная кликабельная область
 * на куске текста даёт слишком мелкую цель и хуже читается скринридером.
 */
@Composable
fun BrandTextLink(
    text: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            append(text)
            append(" ")
            withStyle(SpanStyle(color = AppColors.Accent, fontWeight = FontWeight.SemiBold)) {
                append(actionText)
            }
        },
        style = AppTypography.body.copy(color = AppColors.TextSecondary),
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandTextLinkPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandTextLink(text = "No account?", actionText = "Register", onClick = {})
            BrandTextLink(text = "Already have an account?", actionText = "Sign in", onClick = {})
        }
    }
}
