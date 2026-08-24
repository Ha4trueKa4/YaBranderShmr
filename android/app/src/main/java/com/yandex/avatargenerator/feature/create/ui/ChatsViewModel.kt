package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** Чаще раза в 10 секунд список не перечитываем: см. [ChatsIntent.OnScreenResumed]. */
private val RefreshThrottle = 10.seconds

/**
 * ViewModel экрана чатов: загружает список из [ChatsRepository] и отдаёт его в
 * [ChatsState]. Навигация уходит эффектами — сам экран никуда не ходит.
 */
internal class ChatsViewModel(
    private val repository: ChatsRepository,
    // Время параметром — чтобы троттлинг проверялся юнит-тестом без задержек.
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : BaseViewModel<ChatsIntent, ChatsState, ChatsEffect>(ChatsState()) {

    private var loadJob: Job? = null
    private var lastLoadAtMillis: Long? = null

    init {
        load()
    }

    override suspend fun handleIntent(intent: ChatsIntent, state: ChatsState) {
        when (intent) {
            // Повтор после ошибки — по явному действию пользователя, троттлинг не применяем.
            ChatsIntent.OnRetryClick -> load()
            ChatsIntent.OnScreenResumed -> refresh()
            is ChatsIntent.OnChatClick -> sendEffect(ChatsEffect.OpenChat(intent.chatId))
            ChatsIntent.OnNewProjectClick -> sendEffect(ChatsEffect.OpenCreateProject)
        }
    }

    /**
     * Перечитать список при возврате на вкладку. Скелетон не показываем, и
     * слишком частые вызовы гасим: ON_RESUME прилетает и от шторки, и от
     * системного диалога.
     */
    private fun refresh() {
        // TODO: точечно обновлять по результату навигации (флаг в SavedStateHandle
        //  нав-графа при возврате с экрана проекта) — тогда троттлинг не нужен.
        val last = lastLoadAtMillis
        if (last != null && nowMillis() - last < RefreshThrottle.inWholeMilliseconds) return
        load(showSkeleton = false)
    }

    /**
     * Грузит список отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время похода в репозиторий и тап по
     * кнопке «New project» не сработал бы до конца загрузки.
     */
    private fun load(showSkeleton: Boolean = true) {
        // Метка до запроса, а не после: иначе два ON_RESUME подряд во время
        // висящего запроса дали бы два похода в сеть.
        lastLoadAtMillis = nowMillis()
        // Предыдущую загрузку отменяем, иначе её ответ может приехать поверх свежего.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSkeleton) {
                updateState { copy(isLoading = true, hasError = false, isRefreshFailed = false) }
            }
            repository.getChats().fold(
                onSuccess = { loaded ->
                    updateState {
                        copy(
                            isLoading = false,
                            chats = loaded,
                            hasError = false,
                            isRefreshFailed = false,
                        )
                    }
                },
                onFailure = {
                    // Фоновое обновление не сносит уже показанный список: из-за одного
                    // сетевого блипа экран ошибки стёр бы рабочие данные.
                    updateState {
                        if (chats.isNotEmpty()) {
                            copy(isLoading = false, isRefreshFailed = true)
                        } else {
                            copy(isLoading = false, hasError = true, isRefreshFailed = false)
                        }
                    }
                },
            )
        }
    }
}
