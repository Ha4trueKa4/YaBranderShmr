package com.yandex.avatargenerator.feature.create.data

import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Временный мок списка чатов для разработки UI до готовности бэкенда:
 * отдаёт три чата из макета и имитирует сетевую задержку.
 *
 * @param networkDelayMillis имитация сетевой задержки — чтобы видеть скелетон.
 * @param chats что вернуть; параметр вынесен наружу, чтобы в `@Preview` и при
 *  ручной проверке можно было подсунуть `emptyList()` и посмотреть empty-состояние.
 */
class MockChatsRepository(
    private val networkDelayMillis: Long = 700L,
    private val chats: List<Chat> = defaultChats(),
) : ChatsRepository {

    override suspend fun getChats(): List<Chat> {
        delay(networkDelayMillis)
        return chats
    }

    companion object {

        /** Три чата из макета: свежие сверху. */
        fun defaultChats(now: Instant = Instant.now()): List<Chat> = listOf(
            Chat(id = "chat-3", title = "Chat 3", updatedAt = now.minus(15, ChronoUnit.MINUTES)),
            Chat(id = "chat-2", title = "Chat 2", updatedAt = now.minus(1, ChronoUnit.HOURS)),
            Chat(id = "chat-1", title = "Chat 1", updatedAt = now.minus(1, ChronoUnit.HOURS)),
        )
    }
}
