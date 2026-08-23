package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandBottomSheet
import com.yandex.avatargenerator.ui.components.BrandSelectableOption
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private val DotSize = 6.dp

/**
 * Чип видимости в шапке экрана проекта: цветная точка + подпись «Приватный/
 * Публичный». Точка violet-акцентная у приватного проекта и зелёная у публичного,
 * чтобы состояние читалось до текста. Тап открывает [ProjectVisibilitySheet].
 */
@Composable
internal fun ProjectVisibilityChip(
    isPublic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (isPublic) R.string.project_visibility_public_badge
        else R.string.project_visibility_private_badge,
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppShape.full))
            .background(AppColors.SurfaceVariant)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppShape.full))
            .clickable(
                onClick = onClick,
                onClickLabel = stringResource(R.string.project_visibility_open_description),
            )
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DotSize)
                .clip(CircleShape)
                .background(if (isPublic) AppColors.Success else AppColors.Accent),
        )
        Text(
            text = label,
            style = AppTypography.caption.copy(
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Шторка выбора видимости проекта: общая [BrandBottomSheet] с двумя карточками-
 * опциями. Тап по опции сразу отдаёт выбор наружу — переключение оптимистичное,
 * поэтому шторку закрывает вызывающий экран.
 */
@Composable
internal fun ProjectVisibilitySheet(
    isPublic: Boolean,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrandBottomSheet(
        title = stringResource(R.string.project_visibility_title),
        subtitle = stringResource(R.string.project_visibility_subtitle),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            BrandSelectableOption(
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.project_visibility_private_title),
                description = stringResource(R.string.project_visibility_private_subtitle),
                selected = !isPublic,
                onClick = { onSelect(false) },
            )
            BrandSelectableOption(
                iconRes = R.drawable.ic_globe,
                title = stringResource(R.string.project_visibility_public_title),
                description = stringResource(R.string.project_visibility_public_subtitle),
                selected = isPublic,
                onClick = { onSelect(true) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectVisibilityChipPreview() {
    AvatarGeneratorTheme {
        Row(
            modifier = Modifier
                .background(Color(0xFF0F0F14))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProjectVisibilityChip(isPublic = false, onClick = {})
            ProjectVisibilityChip(isPublic = true, onClick = {})
        }
    }
}
