package com.yandex.avatargenerator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private const val SHIMMER_WIDTH = 300f

/**
 * Горизонтально бегущий градиент-мерцание для плейсхолдеров загрузки
 * (см. "Skeleton Loaders" в Component Sandbox). Один экземпляр [Brush] стоит
 * переиспользовать на несколько [BrandSkeletonBox] в одном списке/сетке —
 * так мерцание остаётся синхронизированным.
 */
@Composable
fun rememberBrandShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "brandShimmer")
    val translate by transition.animateFloat(
        initialValue = -SHIMMER_WIDTH,
        targetValue = SHIMMER_WIDTH * 2,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "brandShimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(AppColors.ShimmerBase, AppColors.ShimmerHighlight, AppColors.ShimmerBase),
        start = Offset(translate, 0f),
        end = Offset(translate + SHIMMER_WIDTH, 0f)
    )
}

/** Плейсхолдер-плашка со скруглением [shape], залитая мерцающим [brush]. */
@Composable
fun BrandSkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush = rememberBrandShimmerBrush(),
    shape: Shape = RoundedCornerShape(AppShape.xs)
) {
    Column(modifier = modifier.clip(shape).background(brush)) {}
}

/**
 * Плейсхолдер строки списка: круглый аватар + две строки текста (заголовок, подпись).
 */
@Composable
fun BrandListItemSkeleton(modifier: Modifier = Modifier, brush: Brush = rememberBrandShimmerBrush()) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        BrandSkeletonBox(modifier = Modifier.size(36.dp), brush = brush, shape = CircleShape)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            BrandSkeletonBox(modifier = Modifier.width(120.dp).height(14.dp), brush = brush)
            BrandSkeletonBox(modifier = Modifier.width(60.dp).height(10.dp), brush = brush)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandSkeletonPreview() {
    AvatarGeneratorTheme {
        val brush = rememberBrandShimmerBrush()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandListItemSkeleton(brush = brush)
            BrandListItemSkeleton(brush = brush)
        }
    }
}
