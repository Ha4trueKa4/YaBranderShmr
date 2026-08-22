package com.yandex.avatargenerator.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * База MVI-экрана: однонаправленный поток `Intent → handleIntent → State/Effect`.
 *
 * Интенты обрабатываются последовательно, по одному из канала, поэтому
 * [handleIntent] всегда видит состояние, актуальное именно для своего интента —
 * гонок «прочитал старое, записал поверх нового» здесь не бывает.
 *
 * Важно: [handleIntent] держит очередь, пока не вернётся. Долгую работу (сеть)
 * ветка обязана запускать отдельной корутиной в `viewModelScope`, иначе
 * набранный за время запроса ввод встанет в очередь и приедет в UI пачкой.
 */
abstract class BaseViewModel<I : BaseIntent, S : BaseState, E : BaseEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    // UNLIMITED: onIntent вызывается из UI и не имеет права ни подвиснуть,
    // ни потерять нажатие.
    private val intents = Channel<I>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (intent in intents) {
                handleIntent(intent, _state.value)
            }
        }
    }

    /** Единственная точка входа для UI. */
    fun onIntent(intent: I) {
        intents.trySend(intent)
    }

    /**
     * Обработка интента. [state] — состояние, актуальное на момент обработки
     * именно этого интента.
     */
    protected abstract suspend fun handleIntent(intent: I, state: S)

    protected fun updateState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected suspend fun sendEffect(effect: E) {
        _effect.send(effect)
    }
}
