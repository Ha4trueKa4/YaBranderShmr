package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

private val BubbleTail = 4.dp

private data class SkeletonBubble(val isOutgoing: Boolean, val width: Dp, val height: Dp)

private val SkeletonBubbles = listOf(
    SkeletonBubble(isOutgoing = false, width = 220.dp, height = 56.dp),
    SkeletonBubble(isOutgoing = true, width = 170.dp, height = 40.dp),
    SkeletonBubble(isOutgoing = false, width = 240.dp, height = 128.dp),
    SkeletonBubble(isOutgoing = true, width = 150.dp, height = 40.dp),
    SkeletonBubble(isOutgoing = false, width = 200.dp, height = 72.dp),
)

/**
 * Скелетон диалога проекта на время первичной загрузки: пузыри-заглушки по краям
 * экрана с мерцанием (shimmer), как в ленте/чатах. Собран из примитивов ДС.
 */
@Composable
internal fun ProjectSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberBrandShimmerBrush()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        items(SkeletonBubbles) { bubble -> BubbleRow(bubble = bubble, brush = brush) }
    }
}

@Composable
private fun BubbleRow(bubble: SkeletonBubble, brush: Brush) {
    val shape = RoundedCornerShape(
        topStart = AppShape.l,
        topEnd = AppShape.l,
        bottomEnd = if (bubble.isOutgoing) BubbleTail else AppShape.l,
        bottomStart = if (bubble.isOutgoing) AppShape.l else BubbleTail,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (bubble.isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .width(bubble.width)
                .height(bubble.height),
            brush = brush,
            shape = shape,
        )
    }
}
