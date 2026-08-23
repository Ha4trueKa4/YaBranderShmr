package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

private val BarHeight = 56.dp
private val IconTouchSize = 40.dp
private val IconSize = 20.dp

/**
 * Верхняя панель экрана: кнопка «назад», заголовок и опциональные действия справа
 * (слот [actions]). Общий каркас для экранов проекта, подписок и им подобных —
 * высота, область нажатия «назад» и нижний разделитель одни на всех. Иконки-действия
 * строим через [BrandTopBarIcon], чтобы совпадали размеры и область нажатия.
 */
@Composable
fun BrandTopBar(
    title: String,
    backContentDescription: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandTopBarIcon(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backContentDescription,
                tint = AppColors.TextPrimary,
                onClick = onBackClick,
            )
            Text(
                text = title,
                style = AppTypography.heading3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppSpacing.xxs),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
                content = actions,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}

/**
 * Кнопка-иконка панели: сама иконка по макету [IconSize], но область нажатия
 * расширена до [IconTouchSize] — иначе в неё трудно попасть пальцем.
 */
@Composable
fun BrandTopBarIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(IconTouchSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(IconSize),
        )
    }
}
