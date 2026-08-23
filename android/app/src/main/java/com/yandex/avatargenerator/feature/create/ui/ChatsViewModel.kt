package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel экрана чатов: загружает список из [ChatsRepository]
 * и отдаёт экрану одно из состояний [ChatsUiState].
 */
internal class ChatsViewModel(
    private val repository: ChatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    /**
     * Перечитать список при возврате на вкладку — пока пользователь был в чате,
     * он мог создать проект. Скелетон не показываем: список уже на экране, и
     * мигать им на каждом возврате незачем.
     */
    fun refresh() = load(showSkeleton = false)

    private fun load(showSkeleton: Boolean = true) {
        // Предыдущую загрузку отменяем, иначе её ответ может приехать поверх свежего.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showSkeleton) _uiState.value = ChatsUiState.Loading
            _uiState.value = repository.getChats().fold(
                onSuccess = { chats ->
                    if (chats.isEmpty()) ChatsUiState.Empty else ChatsUiState.Content(chats)
                },
                onFailure = { ChatsUiState.Error },
            )
        }
    }
}
