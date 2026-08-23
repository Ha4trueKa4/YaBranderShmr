package com.yandex.avatargenerator.feature.subscription.fake

import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/**
 * Репозиторий-подделка для тестов [com.yandex.avatargenerator.feature.subscription.ui.SubscriptionsViewModel].
 *
 * Сети нет: списки и сбои задаются полями, вызовы считаются.
 */
internal class FakeSubscriptionRepository : SubscriptionRepository {

    var followers: List<SubscriptionUser> = listOf(user("f1"), user("f2", following = true))
    var following: List<SubscriptionUser> = listOf(user("g1"))

    /** Если задано — списочные методы бросают это вместо ответа. */
    var listFailure: Exception? = null

    /** Если true — [subscribe]/[unsubscribe] возвращают `Result.failure` (для отката). */
    var subscribeFails: Boolean = false

    var subscribeCallCount: Int = 0
        private set
    var unsubscribeCallCount: Int = 0
        private set

    override suspend fun subscribe(userId: String): Result<Unit> {
        subscribeCallCount++
        return if (subscribeFails) Result.failure(RuntimeException("boom")) else Result.success(Unit)
    }

    override suspend fun unsubscribe(userId: String): Result<Unit> {
        unsubscribeCallCount++
        return if (subscribeFails) Result.failure(RuntimeException("boom")) else Result.success(Unit)
    }

    override suspend fun getFollowers(userId: String): List<SubscriptionUser> {
        listFailure?.let { throw it }
        return followers
    }

    override suspend fun getFollowing(userId: String): List<SubscriptionUser> {
        listFailure?.let { throw it }
        return following
    }

    private companion object {
        fun user(id: String, following: Boolean = false) = SubscriptionUser(
            id = id,
            displayName = "User $id",
            avatarUrl = null,
            followersCount = 100,
            isFollowing = following,
        )
    }
}
