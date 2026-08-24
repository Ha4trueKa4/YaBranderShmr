package com.yandex.avatargenerator.feature.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.core.mvi.CollectEffects
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран профиля пользователя (свой/чужой). Отличие своего профиля от чужого —
 * только кнопка действия ([ProfileState.isOwnProfile]) и шестерёнка настроек.
 *
 * Навигация отдана наружу коллбэками, а внутри она приходит эффектом
 * [ProfileEffect]: тап уходит интентом во ViewModel, и уже она решает, куда
 * вести — в том числе для кнопки шапки, которая на своём профиле открывает
 * редактирование, а на чужом переключает подписку.
 *
 * `userId`, `isOwnProfile` и видимость кнопок известны только при открытии
 * экрана — передаём их параметрами ([parametersOf]); граф — в
 * `feature/profile/di/ProfileModule.kt`.
 *
 * @param onProjectClick открыть проект по id.
 * @param onActionClick клик по кнопке шапки на своём профиле (редактирование);
 *   на чужом кнопка подписки обрабатывается во ViewModel и сюда не доходит.
 * @param onOpenFollowers открыть список подписчиков этого профиля.
 * @param onOpenFollowing открыть список подписок этого профиля.
 * @param onOpenSettings открыть настройки; задаётся только для своего профиля —
 *   тогда в правом верхнем углу показывается кнопка-шестерёнка.
 * @param onBackClick возврат из шапки; null — бара нет. Своя вкладка открыта из
 *   нижней панели, возвращаться там некуда, а чужой профиль — экран-деталь.
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
    onBackClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel {
        // Наличие коллбэка и есть признак «кнопку рисуем»: состояние про null-ность
        // лямбд знать не должно, поэтому передаём во ViewModel готовые флаги.
        parametersOf(userId, isOwnProfile, onBackClick != null, onOpenSettings != null)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is ProfileEffect.OpenProject -> onProjectClick(effect.projectId)
            is ProfileEffect.OpenFollowers -> onOpenFollowers()
            is ProfileEffect.OpenFollowing -> onOpenFollowing()
            ProfileEffect.OpenSettings -> onOpenSettings?.invoke()
            ProfileEffect.OpenEditProfile -> onActionClick()
            ProfileEffect.NavigateBack -> onBackClick?.invoke()
        }
    }

    ProfileContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
