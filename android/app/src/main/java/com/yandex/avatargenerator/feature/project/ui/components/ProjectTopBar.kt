package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

// Точечные размеры шапки по макету, вне шкал AppSpacing/AppShape.
private val BarHeight = 56.dp
private val IconTouchSize = 40.dp
private val IconSize = 20.dp

/**
 * Шапка экрана проекта: «назад», название проекта и две кнопки действий —
 * история версий и «поделиться».
 */
@Composable
internal fun ProjectTopBar(
    title: String,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProjectTopBarIcon(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.project_back_description),
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
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                ProjectTopBarIcon(
                    icon = Icons.Outlined.History,
                    contentDescription = stringResource(R.string.project_history_description),
                    tint = AppColors.TextSecondary,
                    onClick = onHistoryClick,
                )
                ProjectTopBarIcon(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.project_share_description),
                    tint = AppColors.TextSecondary,
                    onClick = onShareClick,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}

/**
 * Кнопка-иконка шапки: сама иконка по макету 20dp, но область нажатия расширена
 * до [IconTouchSize] — иначе в неё трудно попасть пальцем.
 */
@Composable
private fun ProjectTopBarIcon(
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

@Preview(showBackground = true, backgroundColor = 0xFF1A1A24)
@Composable
private fun ProjectTopBarPreview() {
    AvatarGeneratorTheme {
        ProjectTopBar(
            title = "Orion Brand AI",
            onBackClick = {},
            onHistoryClick = {},
            onShareClick = {},
        )
    }
}
