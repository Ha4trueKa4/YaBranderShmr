package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Переключатель вкладок ленты: «All» / «Subscriptions».
 * Активная вкладка выделена цветом и нижним индикатором.
 */
@Composable
fun FeedTabRow(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        FeedTabItem(
            title = stringResource(R.string.feed_tab_all),
            selected = selectedTab == FeedTab.ALL,
            onClick = { onTabSelected(FeedTab.ALL) },
        )
        FeedTabItem(
            title = stringResource(R.string.feed_tab_subscriptions),
            selected = selectedTab == FeedTab.SUBSCRIPTIONS,
            onClick = { onTabSelected(FeedTab.SUBSCRIPTIONS) },
        )
    }
}

@Composable
private fun FeedTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTypography.body.copy(
                color = if (selected) AppColors.TextPrimary else AppColors.TextMuted,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            modifier = Modifier.padding(vertical = AppSpacing.s),
        )
        // Нижний индикатор активной вкладки: 2dp — точечное значение по макету.
        Box(
            modifier = Modifier
                .padding(bottom = AppSpacing.s)
                .height(2.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) AppColors.Accent else Color.Transparent),
        )
    }
}
