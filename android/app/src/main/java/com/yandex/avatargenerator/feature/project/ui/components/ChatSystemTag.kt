package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** 14dp — размер искр по макету, точечное значение вне шкалы отступов. */
private val SparkleSize = 14.dp

/**
 * Системная пилюля в ленте («Project created successfully»): искры и текст
 * в скруглённой плашке с рамкой. Выравнивание по центру строки — на вызывающей
 * стороне (см. `ChatTimeline`).
 */
@Composable
internal fun ChatSystemTag(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppShape.full)

    Row(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, shape)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(SparkleSize),
        )
        Text(
            text = text,
            style = AppTypography.caption.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatSystemTagPreview() {
    AvatarGeneratorTheme {
        ChatSystemTag(text = "Project created successfully", modifier = Modifier.padding(16.dp))
    }
}
