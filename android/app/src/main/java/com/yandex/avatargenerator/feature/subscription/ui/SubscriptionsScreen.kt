package com.yandex.avatargenerator.feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.subscription.ui.components.SubscriptionUserRow
import com.yandex.avatargenerator.feature.subscription.ui.components.SubscriptionsTopBar
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandListItemSkeleton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран подписок пользователя: список подписчиков или подписок (какой именно —
 * задаёт [initialTab], та вкладка, по которой пришли с профиля). Заголовок и
 * запрос зависят от вкладки; переключателя на экране нет — на второй список
 * возвращаются через профиль.
 *
 * Навигация отдана наружу коллбэками; ViewModel — из Koin. `userId`/`initialTab`
 * известны только при открытии — передаём их параметрами ([parametersOf]); граф —
 * в `feature/subscription/di/SubscriptionModule.kt`.
 *
 * @param onBack закрыть экран (стрелка «назад»).
 * @param onUserClick открыть профиль пользователя из списка по его id.
 * @param currentUserId id текущего пользователя — его строка показывается без
 *   кнопки подписки (на себя не подписываются); `null`, если id недоступен.
 */
@Composable
fun SubscriptionsScreen(
    userId: String,
    initialTab: SubscriptionsTab,
    onBack: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    viewModel: SubscriptionsViewModel = koinViewModel { parametersOf(userId, initialTab) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Перезагружаем список при возврате на экран (кроме первого входа — там уже
    // грузит init): пока ходили в чужой профиль, там могли подписаться/отписаться.
    var isFirstResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        SubscriptionsTopBar(
            title = stringResource(
                when (state.tab) {
                    SubscriptionsTab.Followers -> R.string.subscriptions_title_followers
                    SubscriptionsTab.Following -> R.string.subscriptions_title_following
                },
            ),
            onBackClick = onBack,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val list = state.list) {
                is SubscriptionsListState.Loading ->
                    LoadingList(modifier = Modifier.fillMaxSize())

                is SubscriptionsListState.Error ->
                    BrandErrorState(
                        title = stringResource(R.string.subscriptions_error_title),
                        retryLabel = stringResource(R.string.subscriptions_error_retry),
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                is SubscriptionsListState.Content ->
                    if (list.users.isEmpty()) {
                        EmptyList(tab = state.tab, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(AppSpacing.m),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
                        ) {
                            items(items = list.users, key = { it.id }) { user ->
                                SubscriptionUserRow(
                                    user = user,
                                    isSelf = user.id == currentUserId,
                                    onClick = { onUserClick(user.id) },
                                    onToggleFollow = { viewModel.toggleFollow(user) },
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun LoadingList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        repeat(SKELETON_ROWS) {
            BrandListItemSkeleton(modifier = Modifier.padding(top = AppSpacing.s))
        }
    }
}

@Composable
private fun EmptyList(tab: SubscriptionsTab, modifier: Modifier = Modifier) {
    BrandEmptyState(
        title = stringResource(
            when (tab) {
                SubscriptionsTab.Followers -> R.string.subscriptions_empty_followers_title
                SubscriptionsTab.Following -> R.string.subscriptions_empty_following_title
            },
        ),
        subtitle = stringResource(
            when (tab) {
                SubscriptionsTab.Followers -> R.string.subscriptions_empty_followers_subtitle
                SubscriptionsTab.Following -> R.string.subscriptions_empty_following_subtitle
            },
        ),
        modifier = modifier,
    )
}

private const val SKELETON_ROWS = 8
