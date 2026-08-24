package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана чатов — вся навигация отсюда. Отдаются через канал,
 * а не через [ChatsState], чтобы переход не повторялся на каждой рекомпозиции.
 */
internal sealed interface ChatsEffect : BaseEffect {

    /** Открыть чат (он же проект) по [chatId]. */
    data class OpenChat(val chatId: String) : ChatsEffect

    data object OpenCreateProject : ChatsEffect
}
