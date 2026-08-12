package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Текстовое поле формы с поддержкой ошибки валидации и опциональной иконкой в конце поля
 * (например, переключатель видимости пароля) — см. "02 · Form Fields & Input Validation".
 */
@Composable
fun BrandTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorMessage: String? = null,
    height: Dp = 44.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val isError = errorMessage != null
    val borderColor = if (isError) AppColors.Error else AppColors.Border

    Column(modifier = modifier) {
        Text(text = label, style = AppTypography.label)
        Box(
            modifier = Modifier
                .padding(top = AppSpacing.xs)
                .fillMaxWidth()
                .height(height)
                .background(AppColors.SurfaceVariant, RoundedCornerShape(AppShape.m))
                .border(1.dp, borderColor, RoundedCornerShape(AppShape.m))
                .padding(horizontal = AppSpacing.m),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = AppColors.TextMuted, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(AppColors.Accent),
                        keyboardOptions = keyboardOptions,
                        visualTransformation = visualTransformation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AppColors.Error,
                style = AppTypography.caption,
                modifier = Modifier.padding(top = AppSpacing.xxs)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandTextFieldPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandTextField(
                label = "Email",
                value = "",
                onValueChange = {},
                placeholder = "Enter your corporate email..."
            )
            BrandTextField(
                label = "Email",
                value = "invalid-email-address",
                onValueChange = {},
                errorMessage = "Please enter a valid email address."
            )
        }
    }
}
