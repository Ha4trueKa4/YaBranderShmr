package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Сегментированный переключатель (pill segmented control) по макету: равные по
 * ширине сегменты в общей плашке, выбранный залит акцентом. Для взаимоисключающего
 * выбора из нескольких коротких вариантов — тема оформления, режим и т.п.
 *
 * @param options подписи сегментов слева направо.
 * @param selectedIndex индекс выбранного сегмента.
 * @param onSelect выбран сегмент с данным индексом.
 */
@Composable
fun BrandSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppShape.m))
            .background(AppColors.SurfaceVariant)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppShape.m))
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(AppShape.s))
                    .background(if (selected) AppColors.Accent else Color.Transparent)
                    .selectable(
                        selected = selected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = AppTypography.label.copy(
                        color = if (selected) AppColors.OnAccent else AppColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}
