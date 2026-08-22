package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape

/** Толщина рамки поверхностей по макету — одна на все карточки, плашки и тайлы. */
private val BorderWidth = 1.dp

/**
 * Поверхность дизайн-системы: заливка [color] и рамка [borderColor] формы [shape],
 * контент обрезан по той же форме. Карточки, плашки и иконочные тайлы отличаются
 * только скруглением, поэтому геометрия описана здесь, а не в каждом компоненте.
 */
@Composable
fun Modifier.brandSurface(
    shape: Shape = RoundedCornerShape(AppShape.l),
    color: Color = AppColors.Surface,
    borderColor: Color = AppColors.Border,
): Modifier = clip(shape)
    .background(color)
    .border(BorderWidth, borderColor, shape)
