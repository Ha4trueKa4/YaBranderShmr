package com.yandex.avatargenerator.feature.auth.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.components.brandSurface
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape

/**
 * Небольшой квадратный значок бренда над заголовком формы (см. "small-logo" в макете Figma).
 * Общий для экранов входа и регистрации.
 */
@Composable
internal fun AuthBrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .brandSurface(RoundedCornerShape(AppShape.s)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.HighlightOff,
            contentDescription = null,
            tint = AppColors.AccentText,
            modifier = Modifier.size(20.dp)
        )
    }
}
