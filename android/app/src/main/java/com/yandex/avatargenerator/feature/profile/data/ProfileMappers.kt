package com.yandex.avatargenerator.feature.profile.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedAuthor
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto
import com.yandex.avatargenerator.feature.project.data.toInstantOrNull
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDescriptionDto
import com.yandex.avatargenerator.feature.profile.domain.model.ProfileDetails
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile

/**
 * DTO выше `data/` не поднимаются: маппинг живёт здесь.
 *
 * Контракт `GET /users/{id}` беднее макета: `bio`, `avatar` и отдельного
 * `handle` в нём нет. До появления полей на бэкенде — пустые значения
 * (`bio = ""`, `avatarUrl = null`, `handle = null`, UI скрывает недостающее),
 * а не выдумываем данные (имя как никнейм показало бы его дважды).
 */
internal fun UserProfileResponseDto.toDomain(userId: String): ProfileDetails {
    val id = id ?: userId
    val displayName = name.orEmpty()
    val profile = UserProfile(
        id = id,
        displayName = displayName,
        handle = null,
        bio = "",
        avatarUrl = null,
        projectsCount = projects?.size ?: 0,
        followersCount = subscribersCount ?: 0,
        followingCount = subscriptionsCount ?: 0,
        // Для своего профиля поле отсутствует и всё равно игнорируется UI.
        isFollowing = isSubscribed ?: false,
    )
    val author = FeedAuthor(id = id, displayName = displayName, avatarUrl = null)
    val feedProjects = projects.orEmpty().mapNotNull { it.toFeedProjectOrNull(author) }
    return ProfileDetails(profile = profile, projects = feedProjects)
}

/**
 * Проект профиля → карточка ленты. Битый элемент пропускаем, а не роняем весь
 * профиль. Превью — SVG-`url` из S3 (Coil рисует его через `SvgDecoder`); пусто
 * у проекта без сохранённой версии, тогда карточка покажет плейсхолдер.
 */
private fun ProjectDescriptionDto.toFeedProjectOrNull(author: FeedAuthor): FeedProject? {
    val id = id ?: return null
    val name = name ?: return null
    return FeedProject(
        id = id,
        title = name,
        previewUrl = url.orEmpty(),
        author = author,
        publishedAt = createdAt?.toInstantOrNull(),
    )
}
