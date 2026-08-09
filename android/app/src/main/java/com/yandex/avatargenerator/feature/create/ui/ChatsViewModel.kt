package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
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

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ChatsUiState.Loading
            _uiState.value = runCatching { repository.getChats() }.fold(
                onSuccess = { chats ->
                    if (chats.isEmpty()) ChatsUiState.Empty else ChatsUiState.Content(chats)
                },
                onFailure = { ChatsUiState.Error },
            )
        }
    }
}
