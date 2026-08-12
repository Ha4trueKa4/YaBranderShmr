package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Шапка экрана чатов: заголовок «Chats» и нижняя граница.
 *
 * Стрелки «назад» из макета здесь нет намеренно: это корневой экран вкладки,
 * возвращаться некуда (в макет стрелка попала из соседнего фрейма version-history).
 * Поэтому у компонента и нет параметров, кроме [modifier].
 */
@Composable
internal fun ChatsTopBar(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 56.dp — высота шапки по макету, точечное значение вне шкалы отступов.
                .height(56.dp)
                .padding(horizontal = AppSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chats_title),
                style = AppTypography.heading3,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}
