package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Пустое состояние ленты по макету: иконка в круге, заголовок, подзаголовок
 * и опциональная кнопка действия.
 *
 * @param iconRes иконка в круге; null — без иконки.
 * @param actionLabel подпись кнопки; null — без кнопки.
 */
@Composable
fun FeedEmptyState(
    @DrawableRes iconRes: Int?,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l, Alignment.CenterVertically),
    ) {
        if (iconRes != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = title,
                style = AppTypography.heading3,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = AppTypography.body.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null) {
            BrandPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Ошибка первичной загрузки с кнопкой повтора. */
@Composable
fun FeedError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Couldn't load feed",
            style = AppTypography.heading3,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                style = AppTypography.body.copy(
                    color = AppColors.Accent,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/** Индикатор подгрузки следующей страницы (футер masonry-сетки). */
@Composable
fun FeedAppendLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.m),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = AppColors.Accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Ошибка подгрузки следующей страницы с повтором. */
@Composable
fun FeedAppendError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onRetry) {
            Text(
                text = "Failed to load. Retry",
                style = AppTypography.body.copy(color = AppColors.Accent),
            )
        }
    }
}
