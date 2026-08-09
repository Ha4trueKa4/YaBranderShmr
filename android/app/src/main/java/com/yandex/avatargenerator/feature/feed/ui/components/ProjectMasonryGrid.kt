package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yandex.avatargenerator.ui.theme.AppSpacing

/**
 * Общий masonry-контейнер для сеток проектов (лента, профиль): двухколоночный
 * [LazyVerticalStaggeredGrid] с едиными отступами дизайн-системы.
 *
 * Геометрия сетки (число колонок, интервалы) живёт здесь в одном месте, чтобы
 * лента и профиль не расходились. Содержимое ([content]) каждый экран задаёт сам:
 * лента отдаёт пагинируемые карточки + футер догрузки, профиль — шапку, заголовок
 * секции и список карточек. Карточка проекта в обоих — [ProjectCard].
 *
 * @param contentPadding отступы контента: по умолчанию [AppSpacing.m] со всех сторон
 *   (лента); профиль передаёт свои (без верхнего, т.к. обложка шапки уходит под край).
 */
@Composable
fun ProjectMasonryGrid(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.m),
    content: LazyStaggeredGridScope.() -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalItemSpacing = AppSpacing.s,
        content = content,
    )
}
