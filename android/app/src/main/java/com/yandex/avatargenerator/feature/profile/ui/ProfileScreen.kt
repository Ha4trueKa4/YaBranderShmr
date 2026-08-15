package com.yandex.avatargenerator.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.profile.ui.components.ProfileHeader
import com.yandex.avatargenerator.ui.components.BrandSecondaryButton
import com.yandex.avatargenerator.ui.components.ProjectCard
import com.yandex.avatargenerator.ui.components.ProjectMasonryGrid
import com.yandex.avatargenerator.ui.components.masonryAspectRatio
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран профиля пользователя (свой/чужой). Отличие своего профиля от чужого —
 * только кнопка действия ([ProfileUiState.Content.isOwnProfile]).
 *
 * Скроллится весь контент (обложка тоже уезжает): шапка — первый full-line
 * элемент той же masonry-сетки, что и в ленте (переиспользуется [ProjectCard]).
 *
 * Навигация отдана наружу коллбэками; ViewModel берётся из Koin ([koinViewModel]).
 * `userId`/`isOwnProfile` известны только при открытии экрана — передаём их
 * параметрами ([parametersOf]); граф — в `feature/profile/di/ProfileModule.kt`.
 *
 * @param onProjectClick открыть проект по id.
 * @param onActionClick клик по кнопке шапки (Follow / Edit profile).
 */
@Composable
fun ProfileScreen(
    userId: String,
    isOwnProfile: Boolean,
    onProjectClick: (projectId: String) -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel { parametersOf(userId, isOwnProfile) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        when (val current = state) {
            is ProfileUiState.Loading ->
                CircularProgressIndicator(
                    color = AppColors.Accent,
                    modifier = Modifier.align(Alignment.Center),
                )

            is ProfileUiState.Error ->
                ProfileError(onRetry = viewModel::load, modifier = Modifier.align(Alignment.Center))

            is ProfileUiState.Content ->
                ProfileContent(
                    state = current,
                    onProjectClick = onProjectClick,
                    // Чужой профиль — кнопка подписки (обрабатывается ViewModel);
                    // свой — внешний коллбэк (переход к редактированию).
                    onActionClick = {
                        if (current.isOwnProfile) onActionClick() else viewModel.toggleFollow()
                    },
                )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    onProjectClick: (String) -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectMasonryGrid(
        modifier = modifier.fillMaxSize(),
        // Без верхнего отступа: обложка шапки уходит под край экрана.
        contentPadding = PaddingValues(start = AppSpacing.m, end = AppSpacing.m, bottom = AppSpacing.m),
    ) {
        // Карточки отступают от краёв на AppSpacing.m (горизонтальный contentPadding
        // грида), а обложка/дивайдер шапки — доходят до краёв экрана. Через
        // bleedHorizontally «пробиваем» этот отступ у шапки.
        item(span = StaggeredGridItemSpan.FullLine) {
            ProfileHeader(
                profile = state.profile,
                isOwnProfile = state.isOwnProfile,
                onActionClick = onActionClick,
                modifier = Modifier.bleedHorizontally(AppSpacing.m),
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            // Грид уже добавляет verticalItemSpacing с обеих сторон; сверху заголовка
            // добавляем ещё AppSpacing.xxs по макету.
            ProjectsSectionTitle(modifier = Modifier.padding(top = AppSpacing.xxs))
        }

        if (state.projects.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "empty") {
                EmptyProjects(isOwnProfile = state.isOwnProfile)
            }
        } else {
            items(
                items = state.projects,
                key = { it.id },
                contentType = { "project" },
            ) { project ->
                // Автор всех карточек — владелец профиля, он уже показан в шапке;
                // в карточках имя/аватар не дублируем (authorName не передаём).
                ProjectCard(
                    title = project.title,
                    previewUrl = project.previewUrl,
                    aspectRatio = masonryAspectRatio(project.id),
                    onClick = { onProjectClick(project.id) },
                )
            }
        }
    }
}

/** Заглушка на месте сетки, когда у пользователя ещё нет проектов. */
@Composable
private fun EmptyProjects(isOwnProfile: Boolean, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xl),
        text = if (isOwnProfile) "You haven't created any projects yet" else "No projects yet",
        style = AppTypography.body.copy(color = AppColors.TextMuted),
        textAlign = TextAlign.Center,
    )
}

/**
 * Раскрывает full-line элемент грида на всю ширину, «пробивая» горизонтальный
 * contentPadding в [bleed] с каждой стороны: измеряет контент на 2×[bleed] шире
 * доступного слота и сдвигает его влево на [bleed]. Слот в раскладке остаётся
 * прежней ширины, поэтому обложка/дивайдер шапки доходят до краёв экрана, а
 * карточки сохраняют боковой отступ грида.
 */
private fun Modifier.bleedHorizontally(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = (bleed * 2).roundToPx()
    val fullWidth = constraints.maxWidth + extra
    val placeable = measurable.measure(
        constraints.copy(minWidth = fullWidth, maxWidth = fullWidth),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-bleed.roundToPx(), 0)
    }
}

@Composable
private fun ProjectsSectionTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "PROJECTS",
        style = AppTypography.label.copy(color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun ProfileError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(
            text = "Couldn't load profile",
            style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
        )
        BrandSecondaryButton(text = "Retry", onClick = onRetry)
    }
}
