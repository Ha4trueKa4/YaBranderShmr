package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** Чаще раза в 10 секунд список не перечитываем: см. [ChatsViewModel.refresh]. */
private val RefreshThrottle = 10.seconds

/**
 * ViewModel экрана чатов: загружает список из [ChatsRepository]
 * и отдаёт экрану одно из состояний [ChatsUiState].
 */
internal class ChatsViewModel(
    private val repository: ChatsRepository,
    // Время параметром — чтобы троттлинг проверялся юнит-тестом без задержек.
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastLoadAtMillis: Long? = null

    init {
        load()
    }

    /** Повтор после ошибки — по явному действию пользователя, троттлинг не применяем. */
    fun retry() = load()

    /**
     * Перечитать список при возврате на вкладку — пока пользователь был в чате,
     * он мог создать проект. Скелетон не показываем, и слишком частые вызовы
     * гасим: ON_RESUME прилетает и от шторки, и от системного диалога.
     */
    fun refresh() {
        // TODO: точечно обновлять по результату навигации (флаг в SavedStateHandle
        //  нав-графа при возврате с экрана проекта) — тогда троттлинг не нужен.
        val last = lastLoadAtMillis
        if (last != null && nowMillis() - last < RefreshThrottle.inWholeMilliseconds) return
        load(showSkeleton = false)
    }

    private fun load(showSkeleton: Boolean = true) {
        // Метка до запроса, а не после: иначе два ON_RESUME подряд во время
        // висящего запроса дали бы два похода в сеть.
        lastLoadAtMillis = nowMillis()
        // Предыдущую загрузку отменяем, иначе её ответ может приехать поверх свежего.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSkeleton) _uiState.value = ChatsUiState.Loading
            _uiState.value = repository.getChats().fold(
                onSuccess = { chats ->
                    if (chats.isEmpty()) ChatsUiState.Empty else ChatsUiState.Content(chats)
                },
                onFailure = {
                    // Фоновое обновление не сносит уже показанный список: из-за одного
                    // сетевого блипа экран ошибки стёр бы рабочие данные.
                    when (val current = _uiState.value) {
                        is ChatsUiState.Content -> current.copy(isRefreshFailed = true)
                        else -> ChatsUiState.Error
                    }
                },
            )
        }
    }
}
