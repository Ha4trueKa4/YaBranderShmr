package com.yandex.avatargenerator.feature.profile.fake

import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.model.ProfileDetails
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile

/**
 * Репозиторий-подделка для тестов
 * [com.yandex.avatargenerator.feature.profile.ui.ProfileViewModel].
 *
 * @param result что вернуть на очередной вызов; поле `var`, чтобы в тесте retry
 *  подменить ошибку успехом.
 */
internal class FakeProfileRepository(
    var result: Result<ProfileDetails> = Result.success(details()),
) : ProfileRepository {

    var callCount: Int = 0
        private set

    override suspend fun getProfile(userId: String): ProfileDetails {
        callCount++
        return result.getOrThrow()
    }

    companion object {

        fun profile(
            id: String = "user-1",
            isFollowing: Boolean = false,
            followersCount: Int = 100,
        ) = UserProfile(
            id = id,
            displayName = "Sarah Chen",
            handle = "sarahchen",
            bio = "Exploring AI-generated brand identities",
            avatarUrl = null,
            projectsCount = 0,
            followersCount = followersCount,
            followingCount = 45,
            isFollowing = isFollowing,
        )

        fun details(profile: UserProfile = profile()) =
            ProfileDetails(profile = profile, projects = emptyList())
    }
}
