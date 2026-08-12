package com.yandex.avatargenerator.feature.create.domain

import com.yandex.avatargenerator.feature.create.domain.model.Chat

/**
 * Источник списка чатов. Интерфейс живёт в `domain/`, реализации — в `data/`
 * (пока только [com.yandex.avatargenerator.feature.create.data.MockChatsRepository]);
 * при готовности бэкенда добавится сетевая реализация без изменения UI.
 */
interface ChatsRepository {

    /** @return чаты пользователя, свежие сверху; пустой список — чатов ещё нет. */
    suspend fun getChats(): List<Chat>
}
