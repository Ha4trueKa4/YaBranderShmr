package com.yandex.avatargenerator.feature.profile.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedAuthor
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Временный мок профиля для разработки UI до готовности бэкенда.
 * Детерминирован по userId: имя/статистика/проекты стабильны между запусками.
 */
class MockProfileRepository(
    private val networkDelayMillis: Long = 600L,
) : ProfileRepository {

    override suspend fun getProfile(userId: String): UserProfile {
        delay(networkDelayMillis)

        val rnd = Random(userId.hashCode())
        return UserProfile(
            id = userId,
            displayName = "Sarah Chen",
            handle = "sarahchen",
            bio = "Exploring AI-generated brand identities for startups",
            avatarUrl = "https://i.pravatar.cc/150?u=$userId",
            projectsCount = PROJECT_COUNT,
            followersCount = 1_200 + rnd.nextInt(300),
            followingCount = 45,
            isFollowing = false,
        )
    }

    override suspend fun getUserProjects(userId: String): List<FeedProject> {
        delay(networkDelayMillis)

        val author = FeedAuthor(
            id = userId,
            displayName = "Sarah Chen",
            avatarUrl = "https://i.pravatar.cc/150?u=$userId",
        )
        return (0 until PROJECT_COUNT).map { index ->
            val seed = "$userId-$index"
            val rnd = Random(seed.hashCode())
            FeedProject(
                id = seed,
                title = PROJECT_NAMES[rnd.nextInt(PROJECT_NAMES.size)],
                previewUrl = "https://picsum.photos/seed/$seed/600/600",
                author = author,
                publishedAt = Instant.now().minus(index.toLong(), ChronoUnit.DAYS),
            )
        }
    }

    private companion object {
        const val PROJECT_COUNT = 8
        val PROJECT_NAMES = listOf(
            "Hologram Inc", "Echo branding", "Vortex Logo", "Oasis Ventures",
            "Nebula Kit", "Prism Identity", "Aurora Marks", "Flux Studio",
        )
    }
}
