package com.yandex.avatargenerator.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.BrandButtonDefaults
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Скелетон профиля на время первичной загрузки: шапка-заглушка (обложка, аватар,
 * имя, статистика, кнопка) и masonry-сетка карточек-заглушек с мерцанием (shimmer),
 * как в ленте/чатах. Собран из примитивов дизайн-системы ([BrandSkeletonBox]).
 */
@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberBrandShimmerBrush()
    Column(modifier = modifier.fillMaxSize()) {
        HeaderSkeleton(brush = brush)
        HorizontalDivider(color = AppColors.Border)
        GridSkeleton(brush = brush, modifier = Modifier.padding(AppSpacing.m))
    }
}

/** Заглушка шапки: обложка 120dp с аватаром внахлёст (как в [ProfileHeader]) + строки текста и кнопка. */
@Composable
private fun HeaderSkeleton(brush: Brush, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(151.dp),
        ) {
            BrandSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter),
                brush = brush,
                shape = RoundedCornerShape(0.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 89.dp)
                    .clip(CircleShape)
                    .background(AppColors.Background)
                    .padding(3.dp),
            ) {
                BrandSkeletonBox(modifier = Modifier.size(56.dp), brush = brush, shape = CircleShape)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.m,
                    end = AppSpacing.m,
                    top = AppSpacing.s,
                    bottom = AppSpacing.m,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            BrandSkeletonBox(modifier = Modifier.width(160.dp).height(20.dp), brush = brush)
            BrandSkeletonBox(modifier = Modifier.width(100.dp).height(12.dp), brush = brush)
            BrandSkeletonBox(modifier = Modifier.width(220.dp).height(12.dp), brush = brush)
            BrandSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrandButtonDefaults.Height),
                brush = brush,
                shape = RoundedCornerShape(AppShape.m),
            )
        }
    }
}

/** Две колонки карточек-заглушек с разной высотой превью — masonry-эффект как в ленте. */
@Composable
private fun GridSkeleton(brush: Brush, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        SkeletonColumn(thumbHeights = listOf(150.dp, 100.dp, 170.dp), brush = brush, modifier = Modifier.weight(1f))
        SkeletonColumn(thumbHeights = listOf(110.dp, 160.dp, 120.dp), brush = brush, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SkeletonColumn(thumbHeights: List<Dp>, brush: Brush, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        thumbHeights.forEach { height -> SkeletonCard(thumbHeight = height, brush = brush) }
    }
}

/** Карточка-заглушка: превью + строка названия. Без строки автора — на профиле её нет. */
@Composable
private fun SkeletonCard(thumbHeight: Dp, brush: Brush) {
    val shape = RoundedCornerShape(AppShape.l)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface),
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight),
            brush = brush,
            shape = RoundedCornerShape(0.dp),
        )
        Column(modifier = Modifier.padding(AppSpacing.s)) {
            BrandSkeletonBox(
                modifier = Modifier.width(110.dp).height(12.dp),
                brush = brush,
                shape = RoundedCornerShape(AppShape.xs),
            )
        }
    }
}
