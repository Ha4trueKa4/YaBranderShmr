package com.yandex.avatargenerator.feature.create.domain

import com.yandex.avatargenerator.feature.create.domain.model.Chat

/**
 * Источник списка чатов. Интерфейс живёт в `domain/`, реализации — в `data/`
 * ([com.yandex.avatargenerator.feature.create.data.ChatsRepositoryImpl] и мок
 * для `@Preview`).
 */
interface ChatsRepository {

    /**
     * @return чаты текущего пользователя, свежие сверху; пустой список — чатов
     *  ещё нет либо пользователь неизвестен. Ошибку разбирает ViewModel.
     */
    suspend fun getChats(): Result<List<Chat>>
}
