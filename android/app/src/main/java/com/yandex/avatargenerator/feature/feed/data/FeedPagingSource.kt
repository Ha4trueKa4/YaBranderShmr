package com.yandex.avatargenerator.feature.feed.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import kotlinx.coroutines.CancellationException

/**
 * [PagingSource] для одной вкладки ленты. Ключ страницы — курсор с бэкенда
 * (`next_cursor`); первая страница — с `null`. Пагинация forward-only, поэтому
 * `prevKey` всегда `null`, а refresh начинает ленту заново.
 */
class FeedPagingSource(
    private val repository: FeedRepository,
    private val tab: FeedTab,
) : PagingSource<String, FeedProject>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, FeedProject> =
        try {
            val page = repository.getFeed(tab, params.key, params.loadSize)
            LoadResult.Page(
                data = page.projects,
                prevKey = null,
                nextKey = page.nextCursor,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<String, FeedProject>): String? = null
}
