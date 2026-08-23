package com.yandex.avatargenerator.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

private val IconSize = 24.dp
private val MarkSize = 20.dp

/**
 * Карточка одной опции с одиночным выбором — строительный блок пикеров в
 * [BrandBottomSheet] (видимость проекта, язык, выбор модели). Выбранная
 * подсвечивается акцентной рамкой, фоном surfaceVariant и галочкой; невыбранная —
 * нейтральной рамкой и пустым кружком.
 *
 * @param iconRes ведущая иконка; `null` — опция без иконки (например, языки).
 *  Её цвет следует за выбором, чтобы активная опция читалась сразу.
 * @param description вторая строка под заголовком; `null` — только заголовок.
 */
@Composable
fun BrandSelectableOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppShape.l))
            .background(if (selected) AppColors.SurfaceVariant else AppColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) AppColors.Accent else AppColors.Border,
                shape = RoundedCornerShape(AppShape.l),
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.size(IconSize),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = AppTypography.body.copy(fontWeight = FontWeight.SemiBold),
            )
            if (description != null) {
                Text(text = description, style = AppTypography.caption)
            }
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(MarkSize),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(MarkSize)
                    .clip(CircleShape)
                    .border(1.dp, AppColors.Border, CircleShape),
            )
        }
    }
}
