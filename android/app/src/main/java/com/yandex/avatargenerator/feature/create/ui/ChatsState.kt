package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.create.domain.model.Chat

/**
 * Состояние экрана чатов: один immutable-класс со всеми флагами, как требует
 * [BaseState]. Раньше здесь была взаимоисключающая sealed-иерархия
 * `Loading/Empty/Content/Error` — взаимоисключение теперь держится не типом, а
 * порядком веток `when` в [ChatsContent].
 *
 * @property isLoading первичная загрузка или повтор после ошибки — показываем скелетон.
 * @property chats список чатов, свежие сверху.
 * @property hasError загрузка не удалась и показать нечего; пользователю доступен повтор.
 * @property isRefreshFailed фоновое обновление не удалось — список показываем
 *  прежний, а сверху плашку. Сбрасывается успешной загрузкой.
 */
internal data class ChatsState(
    val isLoading: Boolean = true,
    val chats: List<Chat> = emptyList(),
    val hasError: Boolean = false,
    val isRefreshFailed: Boolean = false,
) : BaseState {

    /** Чатов нет — у нового пользователя это нормальный сценарий, не ошибка. */
    val isEmpty: Boolean get() = !isLoading && !hasError && chats.isEmpty()
}
