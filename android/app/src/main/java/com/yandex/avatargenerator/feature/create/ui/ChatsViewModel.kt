package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import kotlinx.coroutines.CancellationException
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
     * Тихая перезагрузка при возврате на экран: скелетон не показываем, при ошибке
     * оставляем текущий список — мог появиться новый чат, пока создавали проект.
     * Первую загрузку (ещё Loading) не трогаем — ею занят [load].
     */
    fun refresh() {
        if (_uiState.value is ChatsUiState.Loading) return
        load(silent = true)
    }

    private fun load(silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _uiState.value = ChatsUiState.Loading
            val next = try {
                val chats = repository.getChats()
                if (chats.isEmpty()) ChatsUiState.Empty else ChatsUiState.Content(chats)
            } catch (cancellation: CancellationException) {
                // Отмена (новой загрузкой) — не ошибка: пробрасываем, иначе записали бы
                // Error поверх актуального состояния.
                throw cancellation
            } catch (failure: Exception) {
                // Тихую перезагрузку в Error не роняем — оставляем текущий список.
                if (silent) return@launch else ChatsUiState.Error
            }
            _uiState.value = next
        }
    }
}
