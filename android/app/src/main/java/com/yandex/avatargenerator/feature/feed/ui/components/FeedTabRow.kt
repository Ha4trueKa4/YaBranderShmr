package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import com.yandex.avatargenerator.feature.feed.ui.theme.FeedColors

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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        FeedTabItem(
            title = "All",
            selected = selectedTab == FeedTab.ALL,
            onClick = { onTabSelected(FeedTab.ALL) },
        )
        FeedTabItem(
            title = "Subscriptions",
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
            color = if (selected) FeedColors.TextPrimary else FeedColors.TextMuted,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        // Нижний индикатор активной вкладки.
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .height(2.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) FeedColors.Accent else Color.Transparent),
        ) {}
    }
}
