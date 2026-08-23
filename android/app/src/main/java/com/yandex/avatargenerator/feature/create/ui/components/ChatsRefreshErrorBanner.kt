package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Плашка «не удалось обновить» под шапкой списка: фоновое обновление упало,
 * но показанный список остался актуальным-насколько-возможно.
 * Полноэкранный `BrandErrorState` здесь не годится — он стирает данные.
 */
@Composable
internal fun ChatsRefreshErrorBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .clickable(
                onClickLabel = stringResource(R.string.chats_error_retry),
                onClick = onRetry,
            )
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.chats_refresh_error),
            style = AppTypography.caption.copy(color = AppColors.Error),
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.chats_error_retry),
            style = AppTypography.caption.copy(color = AppColors.AccentText),
            maxLines = 1,
        )
    }
}
