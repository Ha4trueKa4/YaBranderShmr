package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandTopBar
import com.yandex.avatargenerator.ui.components.BrandTopBarIcon
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Шапка экрана проекта: «назад», название проекта и действия справа — чип
 * видимости, история версий и «поделиться». Каркас — общий [BrandTopBar].
 *
 * @param isPublic видимость проекта для чипа; `null` — проект ещё не загружен
 *  (загрузка/ошибка), чип не показываем.
 */
@Composable
internal fun ProjectTopBar(
    title: String,
    isPublic: Boolean?,
    onVisibilityClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrandTopBar(
        title = title,
        backContentDescription = stringResource(R.string.project_back_description),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        if (isPublic != null) {
            // Иконки-действия высотой 40dp, чип ниже — без явного центрирования
            // он бы прижался к верху общего Row и уехал вверх от их центров.
            ProjectVisibilityChip(
                isPublic = isPublic,
                onClick = onVisibilityClick,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        BrandTopBarIcon(
            icon = Icons.Outlined.History,
            contentDescription = stringResource(R.string.project_history_description),
            tint = AppColors.TextSecondary,
            onClick = onHistoryClick,
        )
        BrandTopBarIcon(
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(R.string.project_share_description),
            tint = AppColors.TextSecondary,
            onClick = onShareClick,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2B3446)
@Composable
private fun ProjectTopBarPreview() {
    AvatarGeneratorTheme {
        ProjectTopBar(
            title = "Orion Brand AI",
            isPublic = false,
            onVisibilityClick = {},
            onBackClick = {},
            onHistoryClick = {},
            onShareClick = {},
        )
    }
}
