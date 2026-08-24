package com.yandex.avatargenerator.feature.createproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.ui.ModelPickerState
import com.yandex.avatargenerator.feature.project.ui.components.ModelChip
import com.yandex.avatargenerator.feature.project.ui.components.ModelPickerSheet
import com.yandex.avatargenerator.feature.project.ui.util.modelTitle
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Вёрстка экрана без ViewModel: всё, что рисуется, приходит в [state], всё, что
 * делает пользователь, уходит одним [onIntent] — так экран рисуется в
 * `@Preview`, не создавая проект.
 */
@Composable
internal fun CreateProjectContent(
    state: CreateProjectState,
    onIntent: (CreateProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .imePadding(),
    ) {
        CreateProjectTopBar(onBack = { onIntent(CreateProjectIntent.OnBackClick) })

        if (state.modelPicker.isSheetVisible) {
            ModelPickerSheet(
                state = state.modelPicker,
                onSelect = { onIntent(CreateProjectIntent.OnModelSelect(it)) },
                onRetry = { onIntent(CreateProjectIntent.OnModelsRetry) },
                onDismiss = { onIntent(CreateProjectIntent.OnModelPickerDismiss) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            // Промпт — первым: это единственное обязательное поле и главное
            // действие экрана, название с описанием под ним необязательны.
            BrandTextField(
                label = stringResource(R.string.create_project_prompt_label),
                value = state.prompt,
                onValueChange = { onIntent(CreateProjectIntent.OnPromptChange(it)) },
                placeholder = stringResource(R.string.create_project_prompt_placeholder),
                singleLine = false,
                height = 140.dp,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Чип модели — под промптом: он про то, чем этот промпт будут
            // рисовать, а не про название с описанием.
            CreateProjectModelField(
                selectedModelId = state.modelPicker.selectedModelId,
                onClick = { onIntent(CreateProjectIntent.OnModelPickerOpen) },
            )

            BrandTextField(
                label = stringResource(R.string.create_project_name_label),
                value = state.name,
                onValueChange = { onIntent(CreateProjectIntent.OnNameChange(it)) },
                placeholder = stringResource(R.string.create_project_name_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            BrandTextField(
                label = stringResource(R.string.create_project_description_label),
                value = state.description,
                onValueChange = { onIntent(CreateProjectIntent.OnDescriptionChange(it)) },
                placeholder = stringResource(R.string.create_project_description_placeholder),
                singleLine = false,
                height = 140.dp,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            Text(
                text = stringResource(R.string.create_project_hint),
                style = AppTypography.caption,
            )
        }

        Column(
            modifier = Modifier.padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            if (state.hasError) {
                Text(
                    text = stringResource(R.string.create_project_error),
                    style = AppTypography.caption,
                    color = AppColors.Error,
                )
            }

            BrandPrimaryButton(
                text = stringResource(
                    if (state.isSubmitting) R.string.create_project_submitting
                    else R.string.create_project_submit,
                ),
                onClick = { onIntent(CreateProjectIntent.OnSubmitClick) },
                fullWidth = true,
                enabled = state.canSubmit,
                leadingIcon = {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppColors.OnAccent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_generate),
                            contentDescription = null,
                            tint = AppColors.OnAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
    }
}

/**
 * Поле выбора модели: подпись как у [BrandTextField] плюс чип-«таблетка» —
 * тот же [ModelChip], что над полем ввода в чате проекта.
 */
@Composable
private fun CreateProjectModelField(
    selectedModelId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.create_project_model_label),
            style = AppTypography.label,
        )
        ModelChip(
            title = modelTitle(selectedModelId),
            onClick = onClick,
            modifier = Modifier.padding(top = AppSpacing.xs),
        )
    }
}

/** Шапка экрана: кнопка «назад» и заголовок с нижней границей. */
@Composable
private fun CreateProjectTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.create_project_back_description),
                    tint = AppColors.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.create_project_title),
                style = AppTypography.heading3,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.Border)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun CreateProjectContentEmptyPreview() {
    AvatarGeneratorTheme {
        CreateProjectContent(state = CreateProjectState(), onIntent = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun CreateProjectContentSubmittingPreview() {
    AvatarGeneratorTheme {
        CreateProjectContent(
            state = CreateProjectState(
                prompt = "Minimal fox mascot, flat vector, deep blue",
                name = "Nova",
                isSubmitting = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun CreateProjectContentPickedModelPreview() {
    AvatarGeneratorTheme {
        CreateProjectContent(
            state = CreateProjectState(
                prompt = "Minimal fox mascot, flat vector, deep blue",
                modelPicker = ModelPickerState(selectedModelId = "gpt-oss-120b/latest"),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun CreateProjectContentErrorPreview() {
    AvatarGeneratorTheme {
        CreateProjectContent(
            state = CreateProjectState(
                prompt = "Minimal fox mascot, flat vector, deep blue",
                name = "Nova",
                hasError = true,
            ),
            onIntent = {},
        )
    }
}
