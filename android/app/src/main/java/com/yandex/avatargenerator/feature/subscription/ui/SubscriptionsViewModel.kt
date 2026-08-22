package com.yandex.avatargenerator.feature.subscription.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана подписок. Грузит список подписчиков или подписок пользователя
 * [userId] — какой именно, задаёт [tab] (та вкладка, по которой пришли с профиля;
 * на самом экране она не переключается, только определяет заголовок и запрос).
 *
 * Кнопка Follow/Unfollow в строке работает оптимистично: перекрашиваем сразу,
 * откатываем при сбое запроса.
 */
class SubscriptionsViewModel(
    private val repository: SubscriptionRepository,
    private val userId: String,
    private val tab: SubscriptionsTab,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionsUiState(tab = tab, list = SubscriptionsListState.Loading),
    )
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    // Пользователи, для которых запрос подписки уже в полёте: повторные тапы по
    // их кнопке игнорируем, иначе встречные subscribe/unsubscribe и их откаты
    // разъехались бы с isFollowing.
    private val followInFlight = mutableSetOf<String>()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Повторная загрузка списка (кнопка «Retry» в состоянии ошибки). */
    fun retry() {
        _uiState.update { it.copy(list = SubscriptionsListState.Loading) }
        load()
    }

    /**
     * Фоновая перезагрузка при возврате на экран: пока пользователь ходил в чужой
     * профиль, он мог там подписаться/отписаться, и список подписок устарел.
     * Скелет не показываем — текущий список остаётся на экране до новых данных.
     */
    fun refresh() {
        load()
    }

    /**
     * Подписаться/отписаться от пользователя [user] через `POST`/`DELETE
     * /users/{id}/subscribe`. UI обновляем оптимистично и откатываем, если запрос
     * не прошёл. Пока запрос в полёте, повторные тапы по этой строке игнорируем.
     */
    fun toggleFollow(user: SubscriptionUser) {
        if (_uiState.value.list !is SubscriptionsListState.Content) return
        if (!followInFlight.add(user.id)) return

        val following = !user.isFollowing
        updateUser(user.id) { it.withFollowing(following) }

        viewModelScope.launch {
            val result = if (following) {
                repository.subscribe(user.id)
            } else {
                repository.unsubscribe(user.id)
            }
            if (result.isFailure) {
                updateUser(user.id) { it.withFollowing(!following) }
            }
            followInFlight.remove(user.id)
        }
    }

    private fun load() {
        // Отменяем прошлую загрузку: при быстрых возвратах её поздний ответ встал
        // бы поверх свежего.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val list = try {
                val users = when (tab) {
                    SubscriptionsTab.Followers -> repository.getFollowers(userId)
                    SubscriptionsTab.Following -> repository.getFollowing(userId)
                }
                SubscriptionsListState.Content(users)
            } catch (cancellation: CancellationException) {
                // Отмена — не ошибка: пробрасываем, иначе записали бы Error поверх
                // актуального списка.
                throw cancellation
            } catch (failure: Exception) {
                SubscriptionsListState.Error
            }
            _uiState.update { it.copy(list = list) }
        }
    }

    private inline fun updateUser(id: String, transform: (SubscriptionUser) -> SubscriptionUser) {
        _uiState.update { state ->
            val content = state.list as? SubscriptionsListState.Content ?: return@update state
            state.copy(
                list = content.copy(
                    users = content.users.map { if (it.id == id) transform(it) else it },
                ),
            )
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
