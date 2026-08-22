package com.yandex.avatargenerator.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.profile.ui.components.ProfileHeader
import com.yandex.avatargenerator.feature.profile.ui.components.ProfileSkeleton
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.components.BrandErrorState
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
 * @param onOpenFollowers открыть список подписчиков этого профиля.
 * @param onOpenFollowing открыть список подписок этого профиля.
 * @param onOpenSettings открыть настройки; задаётся только для своего профиля —
 *   тогда в правом верхнем углу показывается кнопка-шестерёнка.
 */
@Composable
fun ProfileScreen(
    userId: String,
    isOwnProfile: Boolean,
    onProjectClick: (projectId: String) -> Unit,
    onActionClick: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
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
                ProfileSkeleton(modifier = Modifier.fillMaxSize())

            is ProfileUiState.Error ->
                BrandErrorState(
                    title = stringResource(R.string.profile_error_title),
                    retryLabel = stringResource(R.string.profile_error_retry),
                    onRetry = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )

            is ProfileUiState.Content ->
                ProfileContent(
                    state = current,
                    onProjectClick = onProjectClick,
                    onActionClick = {
                        if (current.isOwnProfile) onActionClick() else viewModel.toggleFollow()
                    },
                    onOpenFollowers = onOpenFollowers,
                    onOpenFollowing = onOpenFollowing,
                    onOpenSettings = onOpenSettings,
                )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    onProjectClick: (String) -> Unit,
    onActionClick: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
) {
    ProjectMasonryGrid(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = AppSpacing.m, end = AppSpacing.m, bottom = AppSpacing.m),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            ProfileHeader(
                profile = state.profile,
                isOwnProfile = state.isOwnProfile,
                onActionClick = onActionClick,
                onFollowersClick = onOpenFollowers,
                onFollowingClick = onOpenFollowing,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.bleedHorizontally(AppSpacing.m),
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ProjectsSectionTitle(modifier = Modifier.padding(top = AppSpacing.xxs))
        }

        if (state.projects.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "empty") {
                BrandEmptyState(
                    title = stringResource(
                        if (state.isOwnProfile) R.string.profile_empty_own_title
                        else R.string.profile_empty_other_title,
                    ),
                    subtitle = stringResource(
                        if (state.isOwnProfile) R.string.profile_empty_own_subtitle
                        else R.string.profile_empty_other_subtitle,
                    ),
                    modifier = Modifier.padding(vertical = AppSpacing.xl),
                )
            }
        } else {
            items(
                items = state.projects,
                key = { it.id },
                contentType = { "project" },
            ) { project ->
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
        text = stringResource(R.string.profile_projects_section),
        style = AppTypography.label.copy(color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold),
    )
}
