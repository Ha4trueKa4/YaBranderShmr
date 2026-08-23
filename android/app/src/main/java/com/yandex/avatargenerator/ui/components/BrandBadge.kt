package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Цветовой вариант плашки-бейджа (см. "Badges &amp; Tags" в Component Sandbox). */
enum class BrandBadgeVariant { Accent, Soft, Warm, Success, Neutral }

/** Компактная плашка-тег (статус, категория, метка версии). */
@Composable
fun BrandBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BrandBadgeVariant = BrandBadgeVariant.Neutral
) {
    val (background, foreground) = when (variant) {
        BrandBadgeVariant.Accent -> AppColors.Accent.copy(alpha = 0.15f) to AppColors.AccentText
        BrandBadgeVariant.Soft -> AppColors.AccentSoft.copy(alpha = 0.15f) to AppColors.AccentText
        BrandBadgeVariant.Warm -> AppColors.AccentWarm to AppColors.OnAccentWarm
        BrandBadgeVariant.Success -> AppColors.Success.copy(alpha = 0.15f) to AppColors.Success
        BrandBadgeVariant.Neutral -> AppColors.SurfaceVariant to AppColors.TextSecondary
    }
    Text(
        text = text,
        style = AppTypography.caption.copy(color = foreground, fontWeight = FontWeight.Medium),
        modifier = modifier
            .clip(RoundedCornerShape(AppShape.full))
            .background(background)
            .padding(horizontal = AppSpacing.xs, vertical = 4.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun BrandBadgePreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandBadge(text = "Coral Pro", variant = BrandBadgeVariant.Accent)
                BrandBadge(text = "Creative", variant = BrandBadgeVariant.Soft)
                BrandBadge(text = "Pro", variant = BrandBadgeVariant.Warm)
                BrandBadge(text = "Success", variant = BrandBadgeVariant.Success)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandBadge(text = "Private Workspace", variant = BrandBadgeVariant.Neutral)
                BrandBadge(text = "Original Copy", variant = BrandBadgeVariant.Neutral)
                BrandBadge(text = "Coming Soon", variant = BrandBadgeVariant.Neutral)
            }
        }
    }
}
