package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Общая нижняя шторка приложения: drag handle, фон surface, заголовок с
 * опциональным подзаголовком и слот содержимого. Пикеры на карточках
 * ([BrandSelectableOption]) — видимость проекта, язык, выбор модели — строятся
 * поверх неё, чтобы оболочка (цвета, отступы, скругление) была одна на всех.
 *
 * Содержимое кладётся под шапкой с отступом [AppSpacing.l]; сам стек опций
 * оборачивает вызывающий (обычно `Column` с `spacedBy(AppSpacing.s)`).
 *
 * @param subtitle пояснение под заголовком; `null` — только заголовок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.l)
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(text = title, style = AppTypography.heading3)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTypography.body.copy(color = AppColors.TextSecondary),
                    )
                }
            }
            content()
        }
    }
}
