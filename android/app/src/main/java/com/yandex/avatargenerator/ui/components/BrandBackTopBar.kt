package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private val BarHeight = 56.dp
// Минимальная цель нажатия по Material: с этой кнопки уходят с экрана, мазать мимо
// нельзя. Сама иконка меньше — 20dp по макету.
private val IconTouchSize = 48.dp
private val IconSize = 20.dp

/**
 * Шапка экрана-детали: «назад» и опциональный заголовок.
 *
 * Отличается от `ProjectTopBar` тем, что не несёт действий справа — экранам, где
 * из шапки нужен только возврат (публичный проект, чужой профиль), хватает её.
 * Инсет статус-бара сюда уже приходит от `Scaffold` в `AvatarGeneratorApp`,
 * своего `windowInsetsPadding` бар не добавляет.
 *
 * @param title подпись рядом с кнопкой; null — только кнопка (пока экран грузится
 *   и заголовок ещё неизвестен).
 */
@Composable
fun BrandBackTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(IconTouchSize)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(IconSize),
                )
            }
            if (title != null) {
                Text(
                    text = title,
                    style = AppTypography.heading3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = AppSpacing.xxs),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun BrandBackTopBarPreview() {
    AvatarGeneratorTheme {
        BrandBackTopBar(onBackClick = {}, title = "Пеликан на велосипеде")
    }
}
