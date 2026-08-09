package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Плавающая кнопка «New project» в правом нижнем углу экрана чатов.
 *
 * Собственный компонент, а не `BrandPrimaryButton`: в дизайн-системе у кнопки нет
 * слота под ведущую иконку, а по макету «плюс» обязателен. Геометрия и цвета взяты
 * те же (высота [NewProjectButtonHeight], радиус [AppShape.l], заливка
 * [AppColors.Accent]), чтобы кнопка не выбивалась из остальных.
 */
@Composable
internal fun NewProjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(NewProjectButtonHeight)
            .clip(RoundedCornerShape(AppShape.l))
            .background(AppColors.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            // Декоративная: доступность обеспечивает текст рядом.
            contentDescription = null,
            tint = AppColors.OnAccent,
            // 20.dp — размер иконки по макету, точечное значение вне шкалы отступов.
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.chats_new_project),
            style = AppTypography.body.copy(
                fontWeight = FontWeight.SemiBold,
                color = AppColors.OnAccent,
            ),
        )
    }
}

/** Высота кнопки — та же, что у `BrandPrimaryButton`; нужна экрану для отступа под списком. */
internal val NewProjectButtonHeight: Dp = 48.dp
