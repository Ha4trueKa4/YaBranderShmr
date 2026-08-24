package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import com.yandex.avatargenerator.feature.project.ui.ModelPickerState
import com.yandex.avatargenerator.feature.project.ui.util.modelTitle
import com.yandex.avatargenerator.ui.components.BrandBottomSheet
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandSelectableOption
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

private val ChevronSize = 16.dp

/** Высота ветки загрузки: со спиннером шторка не должна прыгать по высоте. */
private val LoadingHeight = 120.dp

/**
 * Чип выбранной модели над полем ввода: «таблетка» surfaceVariant с рамкой и
 * шевроном — та же оболочка, что у [ProjectVisibilityChip], чтобы низ экрана и
 * шапка читались одинаково. Тап открывает [ModelPickerSheet].
 *
 * Имя модели длинное (а у незнакомой — выведенное из `id`), поэтому строка одна
 * и с многоточием: чип не должен разъезжать поле ввода на две строки.
 */
@Composable
internal fun ModelChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppShape.full))
            .background(AppColors.SurfaceVariant)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppShape.full))
            .clickable(
                onClick = onClick,
                onClickLabel = stringResource(R.string.project_model_open_description),
            )
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(ChevronSize),
        )
        Text(
            text = title,
            style = AppTypography.caption.copy(
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Шторка выбора модели генерации: общая [BrandBottomSheet] со справочником,
 * который тянется лениво при первом открытии.
 *
 * Ошибка загрузки живёт здесь же, а не снэкбаром: она про содержимое шторки и
 * ни на что за её пределами не влияет — отправлять промпты можно и с закрытым
 * справочником.
 */
@Composable
internal fun ModelPickerSheet(
    state: ModelPickerState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrandBottomSheet(
        title = stringResource(R.string.project_model_title),
        subtitle = stringResource(R.string.project_model_subtitle),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        ModelPickerBody(state = state, onSelect = onSelect, onRetry = onRetry)
    }
}

/**
 * Содержимое шторки отдельно от неё самой: `ModalBottomSheet` в `@Preview` не
 * рисуется, а три ветки состояния посмотреть нужно.
 */
@Composable
private fun ModelPickerBody(
    state: ModelPickerState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .height(LoadingHeight),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AppColors.Accent)
        }

        // Пустой список от ошибки не отличаем: выбирать всё равно не из чего.
        state.hasError || state.models.isEmpty() -> BrandErrorState(
            title = stringResource(R.string.project_model_error),
            retryLabel = stringResource(R.string.project_model_retry),
            onRetry = onRetry,
            modifier = modifier,
        )

        else -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            state.models.forEach { model ->
                BrandSelectableOption(
                    title = modelTitle(model.id),
                    description = model.owner,
                    selected = model.id == state.selectedModelId,
                    onClick = { onSelect(model.id) },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ModelChipPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            ModelChip(title = "DeepSeek V4 Flash", onClick = {})
            ModelChip(title = "Qwen3 235B A22B FP8 preview", onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ModelPickerBodyLoadedPreview() {
    AvatarGeneratorTheme {
        Box(modifier = Modifier.padding(AppSpacing.l)) {
            ModelPickerBody(
                state = ModelPickerState(
                    selectedModelId = GenerationModel.DefaultId,
                    models = listOf(
                        GenerationModel(GenerationModel.DefaultId, owner = "DeepSeek"),
                        GenerationModel("gpt-oss-120b/latest", owner = "OpenAI"),
                        GenerationModel("qwen3-235b-a22b-fp8/latest", owner = null),
                    ),
                ),
                onSelect = {},
                onRetry = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ModelPickerBodyLoadingPreview() {
    AvatarGeneratorTheme {
        Box(modifier = Modifier.padding(AppSpacing.l)) {
            ModelPickerBody(
                state = ModelPickerState(isLoading = true),
                onSelect = {},
                onRetry = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ModelPickerBodyErrorPreview() {
    AvatarGeneratorTheme {
        Box(modifier = Modifier.padding(AppSpacing.l)) {
            ModelPickerBody(
                state = ModelPickerState(hasError = true),
                onSelect = {},
                onRetry = {},
            )
        }
    }
}
