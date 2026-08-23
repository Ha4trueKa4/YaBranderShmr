package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Строка версии проекта (см. "Version Rows" в Component Sandbox): заголовок,
 * подпись с датой/автором, опциональный бейдж ([badgeText]) и кнопка "Restore"
 * (скрыта, если [onRestoreClick] не задан — актуальная версия восстанавливать не нужно).
 * Активная версия ([isActive]) подсвечена акцентной полосой слева.
 */
@Composable
fun BrandVersionRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    badgeText: String? = null,
    onRestoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(AppShape.xs))
                .background(if (isActive) AppColors.Accent else Color.Transparent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                Text(
                    text = title,
                    style = AppTypography.body.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
                if (badgeText != null) {
                    BrandBadge(
                        text = badgeText,
                        variant = if (isActive) BrandBadgeVariant.Accent else BrandBadgeVariant.Neutral
                    )
                }
            }
            Text(text = subtitle, style = AppTypography.caption)
        }
        if (onRestoreClick != null) {
            RestoreButton(onClick = onRestoreClick)
        }
    }
}

@Composable
private fun RestoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.brand_version_restore),
        style = AppTypography.label.copy(color = AppColors.TextPrimary),
        modifier = modifier
            .clip(RoundedCornerShape(AppShape.s))
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppShape.s))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun BrandVersionRowPreview() {
    AvatarGeneratorTheme {
        Column {
            BrandVersionRow(
                title = "Version 2.4 (Active)",
                subtitle = "Modified 5 mins ago by Sarah",
                isActive = true,
                badgeText = "Current"
            )
            BrandVersionRow(
                title = "Version 2.3",
                subtitle = "Modified yesterday by Sarah",
                onRestoreClick = {}
            )
            BrandVersionRow(
                title = "Version 1.0 (Original Blueprint)",
                subtitle = "Modified Oct 12, 2025 by System",
                badgeText = "Original",
                onRestoreClick = {}
            )
        }
    }
}
