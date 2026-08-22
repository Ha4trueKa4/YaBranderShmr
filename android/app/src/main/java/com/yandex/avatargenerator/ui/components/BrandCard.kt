package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Карточка превью с заголовком и подписью (см. "Cards / Project Cards" в Component
 * Sandbox) — базовая оболочка для сеток проектов, шаблонов и т.п.
 */
@Composable
fun BrandCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnailUrl: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .brandSurface(RoundedCornerShape(AppShape.l))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(AppColors.SurfaceVariant)
            )
        }
        Column(
            modifier = Modifier.padding(AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Text(
                text = title,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTypography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Плейсхолдер [BrandCard] на время загрузки — те же пропорции, без данных. */
@Composable
fun BrandCardSkeleton(modifier: Modifier = Modifier, brush: Brush = rememberBrandShimmerBrush()) {
    Column(modifier = modifier.brandSurface(RoundedCornerShape(AppShape.l))) {
        BrandSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f),
            brush = brush,
            shape = RoundedCornerShape(0.dp)
        )
        Column(
            modifier = Modifier.padding(AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            BrandSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                brush = brush
            )
            BrandSkeletonBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(10.dp),
                brush = brush
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandCardPreview() {
    AvatarGeneratorTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandCard(
                title = "Neon Gradient Kit",
                subtitle = "By Brandkit AI",
                modifier = Modifier.width(160.dp),
                onClick = {}
            )
            BrandCardSkeleton(modifier = Modifier.width(160.dp))
        }
    }
}
