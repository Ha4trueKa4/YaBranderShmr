package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent

/** Всё, что пользователь может сделать на экране чатов. */
internal sealed interface ChatsIntent : BaseIntent {

    /** Повтор после ошибки. */
    data object OnRetryClick : ChatsIntent

    /**
     * Экран снова на переднем плане: пока пользователь был в чате, он мог
     * создать проект — список стоит перечитать. Частые вызовы гасит троттлинг
     * во [ChatsViewModel].
     */
    data object OnScreenResumed : ChatsIntent

    data class OnChatClick(val chatId: String) : ChatsIntent

    data object OnNewProjectClick : ChatsIntent
}
