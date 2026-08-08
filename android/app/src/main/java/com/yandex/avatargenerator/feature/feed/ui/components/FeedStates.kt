package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.feature.feed.ui.theme.FeedColors

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
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        if (iconRes != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(FeedColors.Surface)
                    .border(1.dp, FeedColors.Border, CircleShape),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = FeedColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FeedColors.Accent)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Couldn't load feed",
            color = FeedColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = "Retry", color = FeedColors.Accent, fontWeight = FontWeight.Medium)
        }
    }
}

/** Индикатор подгрузки следующей страницы (футер masonry-сетки). */
@Composable
fun FeedAppendLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = FeedColors.Accent,
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
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onRetry) {
            Text(text = "Failed to load. Retry", color = FeedColors.Accent)
        }
    }
}
