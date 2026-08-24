package com.yandex.avatargenerator.feature.createproject.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import com.yandex.avatargenerator.feature.create.ui.ChatsScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран создания нового проекта: название, описание и запуск генерации.
 * Открывается из «New project» на экране чатов ([ChatsScreen]).
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [CreateProjectEffect]: переход на проект случается по успешному ответу
 * сервера, когда у проекта уже есть настоящий id.
 *
 * @param onBack закрыть экран (стрелка «назад»).
 * @param onProjectCreated проект создан — открыть его экран по id; вторым
 *  аргументом идёт задача первичной генерации (`null`, если сервер её не
 *  прислал), чтобы экран проекта сразу показал скелетон ответа.
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф — в
 * `feature/createproject/di/CreateProjectModule.kt`.
 */
@Composable
internal fun CreateProjectScreen(
    onBack: () -> Unit,
    onProjectCreated: (projectId: String, jobId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateProjectViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is CreateProjectEffect.NavigateToProject ->
                onProjectCreated(effect.projectId, effect.jobId)
            CreateProjectEffect.NavigateBack -> onBack()
        }
    }

    CreateProjectContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
