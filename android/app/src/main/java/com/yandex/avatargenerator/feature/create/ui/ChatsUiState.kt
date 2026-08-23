package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.feature.create.domain.model.Chat

/**
 * Состояние экрана чатов.
 *
 * Именно `sealed interface`, а не data-класс с флагами: состояния
 * взаимоисключающие, и полноту `when` по ним проверяет компилятор —
 * нельзя случайно забыть ветку.
 */
internal sealed interface ChatsUiState {

    /** Первичная загрузка: показываем скелетон. */
    data object Loading : ChatsUiState

    /** Чатов ещё нет — у нового пользователя это нормальный сценарий. */
    data object Empty : ChatsUiState

    /**
     * Список чатов, свежие сверху.
     *
     * @property isRefreshFailed фоновое обновление не удалось — список показываем
     *  прежний, а сверху плашку. Сбрасывается успешной загрузкой.
     */
    data class Content(
        val chats: List<Chat>,
        val isRefreshFailed: Boolean = false,
    ) : ChatsUiState

    /** Загрузка не удалась; пользователю доступен повтор. */
    data object Error : ChatsUiState
}
