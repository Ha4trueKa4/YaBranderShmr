package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Скелетон списка чатов на время первичной загрузки: три строки-заглушки той же
 * геометрии, что и [ChatRow] (высота 88 = превью 56 + паддинги 2×16), с мерцанием.
 */
@Composable
internal fun ChatsSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

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
        SkeletonBox(
            brush = brush,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(AppShape.s)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            SkeletonBox(
                brush = brush,
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(AppShape.xs)),
            )
            SkeletonBox(
                brush = brush,
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(AppShape.xs)),
            )
        }
    }
}

@Composable
private fun SkeletonBox(brush: Brush, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(brush))
}

/**
 * Горизонтально бегущий градиент-мерцание.
 *
 * Копия приватной функции из `feature/feed/ui/components/FeedSkeleton.kt`:
 * общие shimmer-утилиты появятся, когда в проекте заведут shared-ui модуль,
 * а делать чужую приватную функцию публичной — значит связать две фичи.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -SHIMMER_WIDTH,
        targetValue = SHIMMER_WIDTH * 2,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = listOf(AppColors.ShimmerBase, AppColors.ShimmerHighlight, AppColors.ShimmerBase),
        start = Offset(translate, 0f),
        end = Offset(translate + SHIMMER_WIDTH, 0f),
    )
}

private const val SHIMMER_WIDTH = 300f
private const val SKELETON_ROWS = 3
