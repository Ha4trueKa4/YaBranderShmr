package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandTopBar
import com.yandex.avatargenerator.ui.components.BrandTopBarIcon
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Шапка экрана проекта: «назад», название проекта и две кнопки действий —
 * история версий и «поделиться». Каркас — общий [BrandTopBar].
 */
@Composable
internal fun ProjectTopBar(
    title: String,
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
