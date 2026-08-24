package com.yandex.avatargenerator.feature.subscription.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel экрана подписок. Грузит список подписчиков или подписок пользователя
 * [userId] — какой именно, задаёт `tab` (та вкладка, по которой пришли с профиля;
 * на самом экране она не переключается, только определяет заголовок и запрос).
 *
 * Кнопка Follow/Unfollow в строке работает оптимистично: перекрашиваем сразу,
 * откатываем при сбое запроса.
 */
class SubscriptionsViewModel(
    private val repository: SubscriptionRepository,
    private val userId: String,
    tab: SubscriptionsTab,
) : BaseViewModel<SubscriptionsIntent, SubscriptionsState, SubscriptionsEffect>(
    SubscriptionsState(tab = tab),
) {

    // Пользователи, для которых запрос подписки уже в полёте: повторные тапы по
    // их кнопке игнорируем, иначе встречные subscribe/unsubscribe и их откаты
    // разъехались бы с isFollowing. В состоянии этому не место — экран
    // подсветки «в полёте» не рисует, это чистый гвард.
    private val followInFlight = mutableSetOf<String>()

    private var loadJob: Job? = null

    init {
        load()
    }

    override suspend fun handleIntent(intent: SubscriptionsIntent, state: SubscriptionsState) {
        when (intent) {
            SubscriptionsIntent.OnRetryClick -> {
                updateState { copy(isLoading = true, hasError = false) }
                load()
            }

            // Скелет не показываем — текущий список остаётся на экране до
            // новых данных.
            SubscriptionsIntent.OnScreenResumed -> load()

            is SubscriptionsIntent.OnCurrentUserResolved ->
                updateState { copy(currentUserId = intent.userId) }

            SubscriptionsIntent.OnBackClick -> sendEffect(SubscriptionsEffect.NavigateBack)

            is SubscriptionsIntent.OnUserClick -> sendEffect(
                if (intent.userId == state.currentUserId) {
                    SubscriptionsEffect.OpenOwnProfile
                } else {
                    SubscriptionsEffect.OpenUserProfile(intent.userId)
                },
            )

            is SubscriptionsIntent.OnFollowClick -> toggleFollow(state, intent.user)
        }
    }

    /**
     * Подписаться/отписаться от пользователя [user] через `POST`/`DELETE
     * /users/{id}/subscribe`. UI обновляем оптимистично и откатываем, если запрос
     * не прошёл. Пока запрос в полёте, повторные тапы по этой строке игнорируем.
     */
    private fun toggleFollow(state: SubscriptionsState, user: SubscriptionUser) {
        if (user.id == state.currentUserId) return
        if (state.users.none { it.id == user.id }) return
        if (!followInFlight.add(user.id)) return

        val following = !user.isFollowing
        updateUser(user.id) { it.withFollowing(following) }

        viewModelScope.launch {
            try {
                val result = if (following) {
                    repository.subscribe(user.id)
                } else {
                    repository.unsubscribe(user.id)
                }
                if (result.isFailure) {
                    updateUser(user.id) { it.withFollowing(!following) }
                }
            } finally {
                // finally, а не хвост тела: при отмене корутины (напр. очистка VM)
                // иначе id завис бы в множестве и заблокировал повторные тапы.
                followInFlight.remove(user.id)
            }
        }
    }

    private fun load() {
        // Отменяем прошлую загрузку: при быстрых возвратах её поздний ответ встал
        // бы поверх свежего.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val users = when (state.value.tab) {
                    SubscriptionsTab.Followers -> repository.getFollowers(userId)
                    SubscriptionsTab.Following -> repository.getFollowing(userId)
                }
                updateState { copy(isLoading = false, users = users, hasError = false) }
            } catch (cancellation: CancellationException) {
                // Отмена — не ошибка: пробрасываем, иначе записали бы ошибку поверх
                // актуального списка.
                throw cancellation
            } catch (failure: Exception) {
                updateState { copy(isLoading = false, hasError = true) }
            }
        }
    }

    private inline fun updateUser(
        id: String,
        crossinline transform: (SubscriptionUser) -> SubscriptionUser,
    ) {
        updateState {
            copy(users = users.map { if (it.id == id) transform(it) else it })
        }
    }
}

/**
 * Меняет флаг подписки и подправляет счётчик подписчиков на ±1. Если счётчика нет
 * (бэкенд его не отдаёт), оставляем `null` — подправлять нечего.
 */
private fun SubscriptionUser.withFollowing(following: Boolean): SubscriptionUser =
    copy(
        isFollowing = following,
        followersCount = followersCount?.let { (it + if (following) 1 else -1).coerceAtLeast(0) },
    )
