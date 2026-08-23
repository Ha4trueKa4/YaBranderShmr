package com.yandex.avatargenerator.feature.create.data

import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Мок списка чатов: три чата из макета плюс имитация сетевой задержки.
 * В графе Koin его больше нет (там боевой [ChatsRepositoryImpl]) — остаётся
 * для `@Preview` и ручной проверки состояний экрана.
 *
 * @param networkDelayMillis имитация сетевой задержки — чтобы видеть скелетон.
 * @param chats что вернуть; параметр вынесен наружу, чтобы можно было подсунуть
 *  `emptyList()` и посмотреть empty-состояние.
 */
class MockChatsRepository(
    private val networkDelayMillis: Long = 700L,
    private val chats: List<Chat> = defaultChats(),
) : ChatsRepository {

    override suspend fun getChats(): Result<List<Chat>> {
        delay(networkDelayMillis)
        return Result.success(chats)
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
