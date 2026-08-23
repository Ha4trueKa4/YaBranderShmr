package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Нижняя панель вкладок по макету:
 *  - верхняя разделительная линия 1dp ([AppColors.Border]);
 *  - ряд вкладок высотой 64dp, фон [AppColors.Surface], отступы по бокам 32dp,
 *    вкладки распределены `SpaceBetween`;
 *  - активная вкладка окрашена в [AppColors.AccentText], неактивные — в
 *    [AppColors.TextMuted] (иконка + подпись);
 *  - подсветки нажатия у вкладок нет — обратную связь даёт смена цвета;
 *  - снизу — область системного жеста («хват»), залитая тем же фоном.
 *
 * @param currentDestination текущий узел графа (для подсветки активной вкладки).
 * @param onTabSelected клик по вкладке; хост выполняет переход.
 */
@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface),
    ) {
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { tab ->
                NavItem(
                    tab = tab,
                    selected = currentDestination.isOnRoute(tab.destination.route) ||
                        currentDestination.isOnAnyRoute(tab.detailRoutes),
                    onClick = { onTabSelected(tab) },
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

@Composable
private fun NavItem(
    tab: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint: Color = if (selected) AppColors.AccentText else AppColors.TextMuted
    val label = stringResource(tab.labelRes)

    Column(
        modifier = modifier
            .width(56.dp)
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
