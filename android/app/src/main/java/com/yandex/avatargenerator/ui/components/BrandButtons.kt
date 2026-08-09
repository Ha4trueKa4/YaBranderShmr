package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Основная кнопка призыва к действию (см. "01 · Buttons" в Component Sandbox).
 * @param fullWidth растянуть на всю доступную ширину — вариант "Get Started (Full-Width)".
 */
@Composable
fun BrandPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true
) {
    Material3Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp),
        shape = RoundedCornerShape(AppShape.l),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Accent,
            contentColor = AppColors.OnAccent
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.l)
    ) {
        Text(text = text, color = AppColors.OnAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Второстепенная (outline) кнопка — "Secondary Action" в макете. */
@Composable
fun BrandSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp),
        shape = RoundedCornerShape(AppShape.l),
        border = BorderStroke(1.5.dp, AppColors.Accent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.Accent
        ),
        contentPadding = PaddingValues(horizontal = AppSpacing.l)
    ) {
        Text(text = text, color = AppColors.Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandButtonsPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandPrimaryButton(text = "Get Started", onClick = {})
            BrandPrimaryButton(text = "Get Started (Full-Width)", onClick = {}, fullWidth = true)
            BrandSecondaryButton(text = "Secondary Action", onClick = {})
            BrandPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
