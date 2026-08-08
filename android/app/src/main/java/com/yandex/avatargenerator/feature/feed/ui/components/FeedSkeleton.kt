package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.feature.feed.ui.theme.FeedColors

/**
 * Скелетон ленты на время первичной загрузки: masonry-сетка карточек-заглушек
 * с мерцанием (shimmer). Высоты превью повторяют макет.
 */
@Composable
fun FeedSkeletonGrid(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        thumbHeights.forEach { height -> SkeletonCard(thumbHeight = height, brush = brush) }
    }
}

@Composable
private fun SkeletonCard(thumbHeight: Dp, brush: Brush) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FeedColors.Surface)
            .border(1.dp, FeedColors.Border, RoundedCornerShape(16.dp)),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight),
            brush = brush,
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(110.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp)),
                brush = brush,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    brush = brush,
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    brush = brush,
                )
            }
        }
    }
}

@Composable
private fun SkeletonBox(brush: Brush, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(brush)) {}
}

/** Горизонтально бегущий градиент-мерцание. */
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
        colors = listOf(FeedColors.ShimmerBase, FeedColors.ShimmerHighlight, FeedColors.ShimmerBase),
        start = Offset(translate, 0f),
        end = Offset(translate + SHIMMER_WIDTH, 0f),
    )
}

private const val SHIMMER_WIDTH = 300f
