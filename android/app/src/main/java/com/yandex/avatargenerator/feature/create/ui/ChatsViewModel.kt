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
 *
 * @param nowMillis источник «сейчас» для троттлинга [refresh]; в проде — часы
 *  системы, в тестах — управляемые фейковые часы.
 */
internal class ChatsViewModel(
    private val repository: ChatsRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** Момент последней (завершившейся) загрузки — под троттлинг [refresh]. */
    private var lastLoadedAt: Long = 0L

    init {
        load()
    }

    /** Повтор после ошибки: троттлинг игнорируем. */
    fun retry() = load()

    /**
     * Тихая перезагрузка при возврате на экран: скелетон не показываем. Если
     * обновление упало — оставляем текущий список, но взводим
     * [ChatsUiState.Content.isRefreshFailed]; успех сбрасывает флаг.
     * Частые возвраты не долбим сетью: между загрузками выдерживаем паузу.
     */
    fun refresh() {
        if (_uiState.value is ChatsUiState.Loading) return
        if (nowMillis() - lastLoadedAt < REFRESH_THROTTLE_MILLIS) return
        load(silent = true)
    }

    private fun load(silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _uiState.value = ChatsUiState.Loading
            val next = try {
                val chats = repository.getChats().getOrThrow()
                if (chats.isEmpty()) ChatsUiState.Empty else ChatsUiState.Content(chats)
            } catch (cancellation: CancellationException) {
                // Отмена (новой загрузкой) — не ошибка: пробрасываем, иначе записали бы
                // Error поверх актуального состояния.
                throw cancellation
            } catch (failure: Exception) {
                // Тихую перезагрузку в Error не роняем: список оставляем, но взводим флаг
                // неудачного обновления (если список уже есть), иначе просто выходим.
                if (silent) {
                    val current = _uiState.value
                    if (current is ChatsUiState.Content) {
                        current.copy(isRefreshFailed = true)
                    } else {
                        return@launch
                    }
                } else {
                    ChatsUiState.Error
                }
            }
            _uiState.value = next
            lastLoadedAt = nowMillis()
        }
    }

    private companion object {
        /** Между [refresh] дольше этой паузы загрузок не делаем. */
        const val REFRESH_THROTTLE_MILLIS = 10_000L
    }
}