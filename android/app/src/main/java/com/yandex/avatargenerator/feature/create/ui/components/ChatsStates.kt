package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandSecondaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Пустой список: у нового пользователя чатов ещё нет.
 *
 * Кнопку «New project» здесь намеренно не дублируем — плавающая кнопка
 * экрана и так на виду.
 */
@Composable
internal fun ChatsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.chats_empty_title),
            style = AppTypography.heading3,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.chats_empty_subtitle),
            style = AppTypography.body.copy(color = AppColors.TextSecondary),
            textAlign = TextAlign.Center,
        )
    }
}

/** Ошибка загрузки списка с кнопкой повтора. */
@Composable
internal fun ChatsErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.chats_error_title),
            style = AppTypography.heading3,
            textAlign = TextAlign.Center,
        )
        BrandSecondaryButton(
            text = stringResource(R.string.chats_error_retry),
            onClick = onRetry,
        )
    }
}
