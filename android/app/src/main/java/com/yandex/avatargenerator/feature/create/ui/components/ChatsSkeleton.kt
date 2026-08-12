package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Скелетон списка чатов на время первичной загрузки: три строки-заглушки той же
 * геометрии, что и [ChatRow] (высота 88 = превью 56 + паддинги 2×16), с мерцанием.
 */
@Composable
internal fun ChatsSkeleton(modifier: Modifier = Modifier) {
    // Один brush на все строки — так мерцание в списке остаётся синхронным.
    val brush = rememberBrandShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        repeat(SKELETON_ROWS) { SkeletonRow(brush = brush) }
    }
}

@Composable
private fun SkeletonRow(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppShape.m))
            .background(AppColors.Surface)
            .padding(AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Размеры заглушек точечные — повторяют геометрию реальной строки.
        BrandSkeletonBox(
            brush = brush,
            shape = RoundedCornerShape(AppShape.s),
            modifier = Modifier.size(56.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            BrandSkeletonBox(
                brush = brush,
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp),
            )
            BrandSkeletonBox(
                brush = brush,
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp),
            )
        }
    }
}

private const val SKELETON_ROWS = 3
