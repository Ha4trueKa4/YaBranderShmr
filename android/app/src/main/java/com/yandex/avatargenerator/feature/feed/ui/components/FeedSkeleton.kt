package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.brandSurface
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Скелетон ленты на время первичной загрузки: masonry-сетка карточек-заглушек
 * с мерцанием (shimmer). Высоты превью повторяют макет.
 */
@Composable
fun FeedSkeletonGrid(modifier: Modifier = Modifier) {
    val brush = rememberBrandShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        SkeletonColumn(thumbHeights = listOf(140.dp, 90.dp, 160.dp), brush = brush, modifier = Modifier.weight(1f))
        SkeletonColumn(thumbHeights = listOf(100.dp, 150.dp, 110.dp), brush = brush, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SkeletonColumn(
    thumbHeights: List<Dp>,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        thumbHeights.forEach { height -> SkeletonCard(thumbHeight = height, brush = brush) }
    }
}

@Composable
private fun SkeletonCard(thumbHeight: Dp, brush: Brush) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .brandSurface(RoundedCornerShape(AppShape.l)),
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight),
            brush = brush,
            shape = RoundedCornerShape(0.dp),
        )
        Column(
            modifier = Modifier.padding(AppSpacing.s),
            // Точечные значения без токена (радиус/интервал плейсхолдеров) — по макету.
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BrandSkeletonBox(
                modifier = Modifier
                    .width(110.dp)
                    .height(12.dp),
                brush = brush,
                shape = RoundedCornerShape(AppShape.xs),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandSkeletonBox(
                    modifier = Modifier.size(20.dp),
                    brush = brush,
                    shape = CircleShape,
                )
                BrandSkeletonBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp),
                    brush = brush,
                    shape = RoundedCornerShape(AppShape.xs),
                )
            }
        }
    }
}
