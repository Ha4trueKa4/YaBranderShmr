package com.yandex.avatargenerator.feature.feed.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/**
 * [PagingSource] для одной вкладки ленты. Ключ страницы — её номер (с 0).
 */
class FeedPagingSource(
    private val repository: FeedRepository,
    private val tab: FeedTab,
) : PagingSource<Int, FeedProject>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedProject> {
        val page = params.key ?: STARTING_PAGE
        return try {
            val items = repository.getFeed(tab, page, params.loadSize)
            LoadResult.Page(
                data = items,
                prevKey = if (page == STARTING_PAGE) null else page - 1,
                nextKey = if (items.isEmpty()) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, FeedProject>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor) ?: return null
        // prevKey + 1 или nextKey - 1 дают страницу с якорем; ключ не может быть
        // меньше стартовой страницы.
        return (closest.prevKey?.plus(1) ?: closest.nextKey?.minus(1))
            ?.coerceAtLeast(STARTING_PAGE)
    }

    private companion object {
        const val STARTING_PAGE = 0
    }
}
