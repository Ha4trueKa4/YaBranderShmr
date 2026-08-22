package com.yandex.avatargenerator.feature.subscription.data

import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Временный мок подписок для разработки UI до готовности бэкенда (ручки списков
 * ещё нет, subscribe отдаёт 500 — см. `API.md`). Детерминирован по userId:
 * списки стабильны между запусками.
 */
class MockSubscriptionRepository(
    private val networkDelayMillis: Long = 600L,
) : SubscriptionRepository {

    override suspend fun subscribe(userId: String): Result<Unit> {
        delay(networkDelayMillis)
        return Result.success(Unit)
    }

    override suspend fun unsubscribe(userId: String): Result<Unit> {
        delay(networkDelayMillis)
        return Result.success(Unit)
    }

    override suspend fun getFollowers(userId: String): List<SubscriptionUser> =
        users(seed = "$userId-followers", count = 12)

    override suspend fun getFollowing(userId: String): List<SubscriptionUser> =
        users(seed = "$userId-following", count = 7)

    private suspend fun users(seed: String, count: Int): List<SubscriptionUser> {
        delay(networkDelayMillis)
        val rnd = Random(seed.hashCode())
        return (0 until count).map { index ->
            val name = NAMES[rnd.nextInt(NAMES.size)]
            val id = "$seed-$index"
            SubscriptionUser(
                id = id,
                displayName = name,
                avatarUrl = "https://i.pravatar.cc/150?u=$id",
                followersCount = rnd.nextInt(8_000),
                isFollowing = rnd.nextBoolean(),
            )
        }
    }

    private companion object {
        val NAMES = listOf(
            "Sarah Chen", "Marcus Lee", "Aisha Khan", "Diego Ramos", "Nina Petrova",
            "Liam O'Brien", "Yuki Tanaka", "Omar Farouk", "Elena Rossi", "Noah Kim",
        )
    }
}
