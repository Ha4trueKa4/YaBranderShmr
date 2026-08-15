package com.yandex.avatargenerator.feature.createproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTextField
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран создания нового проекта: название, описание и запуск генерации.
 * Открывается из «New project» на экране чатов ([ChatsScreen]).
 *
 * Навигация отдана наружу коллбэками — владелец графа их прокинет:
 * @param onBack закрыть экран (стрелка «назад»).
 * @param onProjectCreated проект создан — открыть его экран по id.
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф — в
 * `feature/createproject/di/CreateProjectModule.kt`.
 */
@Composable
internal fun CreateProjectScreen(
    onBack: () -> Unit,
    onProjectCreated: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateProjectViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Одноразовые события навигации собираем только пока экран не ниже STARTED —
    // иначе событие, пришедшее в фоне (генерация занимает ~1.5 с), дёрнуло бы
    // navigate на остановленном NavController.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is CreateProjectEvent.ProjectCreated -> onProjectCreated(event.projectId)
                }
            }
        }
    }

    CreateProjectContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

/**
 * Вёрстка экрана без ViewModel: состояние приходит параметром — так экран
 * рисуется в `@Preview`, не создавая проект.
 */
@Composable
private fun CreateProjectContent(
    uiState: CreateProjectUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        CreateProjectTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            BrandTextField(
                label = stringResource(R.string.create_project_name_label),
                value = uiState.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.create_project_name_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            BrandTextField(
                label = stringResource(R.string.create_project_description_label),
                value = uiState.description,
                onValueChange = onDescriptionChange,
                placeholder = stringResource(R.string.create_project_description_placeholder),
                singleLine = false,
                // 140.dp — минимальная высота поля-описания по макету, точечное значение вне шкалы отступов.
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
            if (uiState.hasError) {
                Text(
                    text = stringResource(R.string.create_project_error),
                    style = AppTypography.caption,
                    color = AppColors.Error,
                )
            }

            BrandPrimaryButton(
                text = stringResource(
                    if (uiState.isSubmitting) R.string.create_project_submitting
                    else R.string.create_project_submit,
                ),
                onClick = onSubmit,
                fullWidth = true,
                enabled = uiState.canSubmit,
                leadingIcon = {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            // 18.dp / 2.dp — размеры индикатора внутри кнопки, точечные значения вне шкалы.
                            modifier = Modifier.size(18.dp),
                            color = AppColors.OnAccent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_generate),
                            // Декоративная: доступность обеспечивает текст рядом.
                            contentDescription = null,
                            tint = AppColors.OnAccent,
                            // 20.dp — размер иконки по макету, точечное значение вне шкалы отступов.
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
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
                // 56.dp — высота шапки по макету, точечное значение вне шкалы отступов.
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
