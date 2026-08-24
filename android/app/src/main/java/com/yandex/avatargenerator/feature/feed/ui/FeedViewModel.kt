package com.yandex.avatargenerator.feature.feed.ui

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.feed.data.FeedPagingSource
import com.yandex.avatargenerator.feature.feed.data.FeedRepository
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * ViewModel экрана ленты. Хранит выбранную вкладку в [FeedState] и отдаёт
 * постраничный поток проектов, пересобирая пейджер при смене вкладки.
 *
 * [pagingData] — единственное, что живёт рядом с состоянием, а не внутри него:
 * `PagingData` мутабелен и не сравним, в immutable [FeedState] его не положить
 * (см. KDoc состояния). Экран собирает его отдельно, через
 * `collectAsLazyPagingItems()`.
 */
class FeedViewModel(
    private val repository: FeedRepository,
) : BaseViewModel<FeedIntent, FeedState, FeedEffect>(FeedState()) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<FeedProject>> = state
        .map { it.selectedTab }
        // Пейджер пересобираем только на смене вкладки: без этого любое будущее
        // поле состояния сбрасывало бы ленту в начало.
        .distinctUntilChanged()
        .flatMapLatest { tab ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = { FeedPagingSource(repository, tab) },
            ).flow
        }
        .cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: FeedIntent, state: FeedState) {
        when (intent) {
            is FeedIntent.OnTabSelected -> updateState { copy(selectedTab = intent.tab) }
            is FeedIntent.OnProjectClick -> sendEffect(FeedEffect.OpenProject(intent.projectId))
            is FeedIntent.OnAuthorClick -> sendEffect(FeedEffect.OpenAuthor(intent.authorId))
            FeedIntent.OnExploreClick -> updateState { copy(selectedTab = FeedTab.ALL) }
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
