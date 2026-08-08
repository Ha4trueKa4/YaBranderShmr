package com.yandex.avatargenerator.feature.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yandex.avatargenerator.feature.feed.data.FeedPagingSource
import com.yandex.avatargenerator.feature.feed.data.FeedRepository
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * ViewModel экрана ленты. Хранит выбранную вкладку и отдаёт постраничный поток
 * проектов, пересобирая пейджер при смене вкладки.
 */
class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FeedTab.ALL)
    val selectedTab: StateFlow<FeedTab> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<FeedProject>> = _selectedTab
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

    fun selectTab(tab: FeedTab) {
        _selectedTab.value = tab
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
