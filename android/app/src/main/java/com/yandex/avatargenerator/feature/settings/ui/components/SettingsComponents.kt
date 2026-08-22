package com.yandex.avatargenerator.feature.settings.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandSegmentedControl
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/** Заголовок секции настроек («АККАУНТ», «ОФОРМЛЕНИЕ»…), выровнен по краю карточки. */
@Composable
internal fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.label.copy(color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold),
        modifier = modifier.padding(start = AppSpacing.xxs, top = AppSpacing.m, bottom = AppSpacing.xs),
    )
}

/** Карточка-группа настроек: скруглённая поверхность с бордером; внутри — строки и разделители. */
@Composable
internal fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppShape.m)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, shape),
        content = content,
    )
}

/**
 * Строка настройки: значок в плашке, подпись и опционально значение справа и/или
 * шеврон. Кликабельна, если задан [onClick].
 */
@Composable
internal fun SettingsRow(
    @DrawableRes iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        SettingsIconBadge(iconRes)
        Text(
            text = label,
            style = AppTypography.body.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (valueText != null) {
            Text(
                text = valueText,
                style = AppTypography.body.copy(color = AppColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Выбор темы: подпись со значком и сегментированный переключатель под ней
 * (инлайн, без всплывающего пикера).
 */
@Composable
internal fun SettingsThemeSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            SettingsIconBadge(R.drawable.ic_palette)
            Text(text = label, style = AppTypography.body.copy(fontWeight = FontWeight.Medium))
        }
        BrandSegmentedControl(options = options, selectedIndex = selectedIndex, onSelect = onSelect)
    }
}

/** Значок настройки в квадратной плашке 32×32 (по макету). */
@Composable
private fun SettingsIconBadge(@DrawableRes iconRes: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(AppShape.s))
            .background(AppColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
