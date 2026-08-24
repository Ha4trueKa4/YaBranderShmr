package com.yandex.avatargenerator.feature.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран ленты: хедер + вкладки «All»/«Subscriptions» + masonry-сетка проектов.
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [FeedEffect]: тап уходит интентом во ViewModel, и уже она решает, куда вести.
 *
 * @param onProjectClick открыть проект по id.
 * @param onAuthorClick открыть профиль автора по id.
 *
 * Список собирается отдельно от состояния — `PagingData` в [FeedState] не
 * укладывается (см. его KDoc).
 *
 * ViewModel берётся из Koin ([koinViewModel]); граф зависимостей — в
 * `feature/feed/di/FeedModule.kt`.
 */
@Composable
fun FeedScreen(
    onProjectClick: (projectId: String) -> Unit,
    onAuthorClick: (authorId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects = viewModel.pagingData.collectAsLazyPagingItems()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is FeedEffect.OpenProject -> onProjectClick(effect.projectId)
            is FeedEffect.OpenAuthor -> onAuthorClick(effect.authorId)
        }
    }

    FeedContent(
        state = state,
        projects = projects,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
