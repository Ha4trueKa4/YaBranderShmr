package com.yandex.avatargenerator.feature.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.feed.data.MockFeedRepository
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import com.yandex.avatargenerator.feature.feed.ui.components.FeedAppendError
import com.yandex.avatargenerator.feature.feed.ui.components.FeedAppendLoading
import com.yandex.avatargenerator.feature.feed.ui.components.FeedSkeletonGrid
import com.yandex.avatargenerator.feature.feed.ui.components.FeedTabRow
import com.yandex.avatargenerator.feature.feed.ui.components.FeedTopBar
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.ProjectCard
import com.yandex.avatargenerator.ui.components.ProjectMasonryGrid
import com.yandex.avatargenerator.ui.components.masonryAspectRatio
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Экран ленты: хедер + вкладки «All»/«Subscriptions» + masonry-сетка проектов.
 *
 * Навигация отдана наружу коллбэками — владелец Scaffold/навигации прокинет их:
 * @param onProjectClick открыть проект по id.
 * @param onAuthorClick открыть профиль автора по id.
 *
 * ViewModel создаётся дефолтной фабрикой с моком (пока без DI). При появлении
 * навигационного графа/DI фабрику передадут снаружи.
 */
@Composable
fun FeedScreen(
    onProjectClick: (projectId: String) -> Unit,
    onAuthorClick: (authorId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModelFactory(MockFeedRepository())),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val projects = viewModel.pagingData.collectAsLazyPagingItems()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        FeedTopBar(onSearchClick = { /* TODO: экран/боттомшит поиска */ })
        FeedTabRow(selectedTab = selectedTab, onTabSelected = viewModel::selectTab)
        HorizontalDivider(color = AppColors.Border)

        FeedContent(
            projects = projects,
            selectedTab = selectedTab,
            onProjectClick = onProjectClick,
            onAuthorClick = onAuthorClick,
            onExplore = { viewModel.selectTab(FeedTab.ALL) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FeedContent(
    projects: LazyPagingItems<FeedProject>,
    selectedTab: FeedTab,
    onProjectClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (projects.loadState.refresh) {
        is LoadState.Loading -> FeedSkeletonGrid(modifier)
        is LoadState.Error -> CenteredState(modifier) {
            BrandErrorState(
                title = stringResource(R.string.feed_error_title),
                retryLabel = stringResource(R.string.feed_error_retry),
                onRetry = projects::retry,
            )
        }

        is LoadState.NotLoading -> {
            if (projects.itemCount == 0) {
                FeedEmpty(selectedTab = selectedTab, onExplore = onExplore, modifier = modifier)
            } else {
                FeedGrid(projects, onProjectClick, onAuthorClick, modifier)
            }
        }
    }
}

/** Пустое состояние в зависимости от вкладки (см. макет для «Subscriptions»). */
@Composable
private fun FeedEmpty(
    selectedTab: FeedTab,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(modifier) {
        when (selectedTab) {
            FeedTab.SUBSCRIPTIONS -> BrandEmptyState(
                title = stringResource(R.string.feed_empty_subscriptions_title),
                subtitle = stringResource(R.string.feed_empty_subscriptions_subtitle),
                iconRes = R.drawable.ic_user_plus,
                actionLabel = stringResource(R.string.feed_empty_subscriptions_action),
                onAction = onExplore,
            )

            FeedTab.ALL -> BrandEmptyState(
                title = stringResource(R.string.feed_empty_projects_title),
                subtitle = stringResource(R.string.feed_empty_projects_subtitle),
            )
        }
    }
}

/** Центрирует состояние экрана (пустое/ошибка) на всей свободной площади ленты. */
@Composable
private fun CenteredState(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun FeedGrid(
    projects: LazyPagingItems<FeedProject>,
    onProjectClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectMasonryGrid(modifier = modifier) {
        items(
            count = projects.itemCount,
            key = projects.itemKey { it.id },
            contentType = projects.itemContentType { "project" },
        ) { index ->
            val project = projects[index] ?: return@items
            ProjectCard(
                title = project.title,
                authorName = project.author.displayName,
                previewUrl = project.previewUrl,
                authorAvatarUrl = project.author.avatarUrl,
                aspectRatio = masonryAspectRatio(project.id),
                onClick = { onProjectClick(project.id) },
                onAuthorClick = { onAuthorClick(project.author.id) },
            )
        }

        when (projects.loadState.append) {
            is LoadState.Loading ->
                item(span = StaggeredGridItemSpan.FullLine) { FeedAppendLoading() }

            is LoadState.Error ->
                item(span = StaggeredGridItemSpan.FullLine) {
                    FeedAppendError(onRetry = projects::retry)
                }

            is LoadState.NotLoading -> Unit
        }
    }
}
