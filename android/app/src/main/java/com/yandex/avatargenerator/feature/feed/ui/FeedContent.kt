package com.yandex.avatargenerator.feature.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yandex.avatargenerator.R
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
 * Вёрстка ленты без ViewModel: хедер + вкладки + masonry-сетка проектов.
 *
 * [projects] приходит отдельным параметром, а не полем [state]: `PagingData`
 * мутабелен и не сравним, в immutable-состояние он не укладывается (см. KDoc
 * [FeedState]). Загрузка, ошибка и ретрай списка — тоже из Paging
 * (`loadState`, `retry()`), а не из состояния экрана.
 */
@Composable
internal fun FeedContent(
    state: FeedState,
    projects: LazyPagingItems<FeedProject>,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        FeedTopBar(onSearchClick = { /* TODO: экран/боттомшит поиска */ })
        FeedTabRow(
            selectedTab = state.selectedTab,
            onTabSelected = { onIntent(FeedIntent.OnTabSelected(it)) },
        )
        HorizontalDivider(color = AppColors.Border)

        FeedList(
            projects = projects,
            selectedTab = state.selectedTab,
            onIntent = onIntent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FeedList(
    projects: LazyPagingItems<FeedProject>,
    selectedTab: FeedTab,
    onIntent: (FeedIntent) -> Unit,
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
                FeedEmpty(selectedTab = selectedTab, onIntent = onIntent, modifier = modifier)
            } else {
                FeedGrid(projects, onIntent, modifier)
            }
        }
    }
}

/** Пустое состояние в зависимости от вкладки (см. макет для «Subscriptions»). */
@Composable
private fun FeedEmpty(
    selectedTab: FeedTab,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(modifier) {
        when (selectedTab) {
            FeedTab.SUBSCRIPTIONS -> BrandEmptyState(
                title = stringResource(R.string.feed_empty_subscriptions_title),
                subtitle = stringResource(R.string.feed_empty_subscriptions_subtitle),
                iconRes = R.drawable.ic_user_plus,
                actionLabel = stringResource(R.string.feed_empty_subscriptions_action),
                onAction = { onIntent(FeedIntent.OnExploreClick) },
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
    onIntent: (FeedIntent) -> Unit,
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
                onClick = { onIntent(FeedIntent.OnProjectClick(project.id)) },
                onAuthorClick = { onIntent(FeedIntent.OnAuthorClick(project.author.id)) },
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
