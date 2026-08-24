package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Верхняя панель ленты: словомарка бренда слева и круглая кнопка поиска справа.
 * Словомарка выбирается по теме: белый текст для тёмного фона, тёмный — для светлого.
 */
@Composable
fun FeedTopBar(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(
                if (AppColors.IsDark) R.drawable.ic_brand_logo_dark
                else R.drawable.ic_brand_logo_light
            ),
            contentDescription = stringResource(R.string.feed_brand_name),
            modifier = Modifier.height(24.dp),
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceVariant)
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.feed_search_description),
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}