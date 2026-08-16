package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandButtonDefaults
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

private val HeroHeight = 300.dp
private val RelatedPreviewHeight = 100.dp

/**
 * Скелетон публичного проекта на время первичной загрузки: hero-заглушка, строка
 * автора, заголовок, кнопки экспорта и сетка работ — с мерцанием (shimmer), как в
 * ленте/чатах. Собран из примитивов дизайн-системы ([BrandSkeletonBox]).
 */
@Composable
internal fun PublicProjectSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberBrandShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight),
            brush = brush,
            shape = RoundedCornerShape(0.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            AuthorRowSkeleton(brush = brush)

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                BrandSkeletonBox(modifier = Modifier.width(220.dp).height(24.dp), brush = brush)
                BrandSkeletonBox(modifier = Modifier.width(120.dp).height(12.dp), brush = brush)
            }

            BrandSkeletonBox(
                modifier = Modifier.fillMaxWidth().height(BrandButtonDefaults.Height),
                brush = brush,
                shape = RoundedCornerShape(AppShape.m),
            )
            BrandSkeletonBox(
                modifier = Modifier.fillMaxWidth().height(BrandButtonDefaults.Height),
                brush = brush,
                shape = RoundedCornerShape(AppShape.m),
            )

            HorizontalDivider(thickness = 1.dp, color = AppColors.Border)

            BrandSkeletonBox(modifier = Modifier.width(160.dp).height(14.dp), brush = brush)
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    RelatedCardSkeleton(brush = brush, modifier = Modifier.weight(1f))
                    RelatedCardSkeleton(brush = brush, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AuthorRowSkeleton(brush: Brush, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandSkeletonBox(modifier = Modifier.size(40.dp), brush = brush, shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                BrandSkeletonBox(modifier = Modifier.width(120.dp).height(14.dp), brush = brush)
                BrandSkeletonBox(modifier = Modifier.width(80.dp).height(10.dp), brush = brush)
            }
        }
        BrandSkeletonBox(
            modifier = Modifier.width(76.dp).height(28.dp),
            brush = brush,
            shape = RoundedCornerShape(AppShape.full),
        )
    }
}

@Composable
private fun RelatedCardSkeleton(brush: Brush, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppShape.m)
    Column(
        modifier = modifier
            .background(AppColors.Surface, shape),
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(RelatedPreviewHeight),
            brush = brush,
            shape = RoundedCornerShape(topStart = AppShape.m, topEnd = AppShape.m, bottomStart = 0.dp, bottomEnd = 0.dp),
        )
        Column(
            modifier = Modifier.padding(
                start = AppSpacing.s,
                end = AppSpacing.s,
                top = AppSpacing.xs,
                bottom = AppSpacing.s,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            BrandSkeletonBox(modifier = Modifier.width(90.dp).height(12.dp), brush = brush)
            BrandSkeletonBox(modifier = Modifier.width(60.dp).height(10.dp), brush = brush)
        }
    }
}
