package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

// Футер masonry-сетки: состояния подгрузки следующей страницы.
// Первичные пустое состояние и ошибка живут в дизайн-системе (BrandEmptyState /
// BrandErrorState) — здесь осталась только специфика пагинации ленты.

/** Индикатор подгрузки следующей страницы. */
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
                text = stringResource(R.string.feed_append_error_retry),
                style = AppTypography.body.copy(color = AppColors.Accent),
            )
        }
    }
}
