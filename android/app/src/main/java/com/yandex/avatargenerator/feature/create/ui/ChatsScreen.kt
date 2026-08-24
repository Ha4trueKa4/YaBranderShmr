package com.yandex.avatargenerator.feature.create.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран вкладки «Создать»: список чатов пользователя (чат = диалог с ИИ = проект)
 * и плавающая кнопка «New project».
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [ChatsEffect]: тап уходит интентом во ViewModel, и уже она решает, куда вести.
 *
 * @param onChatClick открыть чат (он же проект) по id.
 * @param onNewProject создать новый проект.
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф — в
 * `feature/create/di/ChatsModule.kt`. Про инсеты думать не нужно:
 * `AvatarGeneratorApp` уже отдаёт `innerPadding` в `NavHost`, а нижняя
 * панель сама обрабатывает `WindowInsets.navigationBars`.
 */
@Composable
internal fun ChatsScreen(
    onChatClick: (chatId: String) -> Unit,
    onNewProject: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is ChatsEffect.OpenChat -> onChatClick(effect.chatId)
            ChatsEffect.OpenCreateProject -> onNewProject()
        }
    }

    // Перечитываем список при возврате на вкладку: пока пользователь был в чате,
    // он мог создать проект. Частые ON_RESUME гасит троттлинг во ViewModel.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
    }

    ChatsContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
