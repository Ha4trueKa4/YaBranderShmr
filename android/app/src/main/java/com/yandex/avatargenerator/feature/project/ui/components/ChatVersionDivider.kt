package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Разделитель ленты с отметкой сохранённой версии — «VERSION 1.2 SAVED»
 * акцентным цветом между двумя линиями.
 *
 * Регистр повышаем здесь, а не в `strings.xml`: в ресурсе строка остаётся
 * человекочитаемой («Version %1$s saved»), верхний регистр — оформление.
 */
@Composable
internal fun ChatVersionDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = AppColors.Border)
        Text(
            text = text.uppercase(),
            style = AppTypography.caption.copy(
                color = AppColors.AccentCyan,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = AppColors.Border)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatVersionDividerPreview() {
    AvatarGeneratorTheme {
        ChatVersionDivider(text = "Version 1.2 saved", modifier = Modifier.padding(16.dp))
    }
}
