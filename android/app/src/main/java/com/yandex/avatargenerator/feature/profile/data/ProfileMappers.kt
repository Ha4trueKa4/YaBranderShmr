package com.yandex.avatargenerator.feature.profile.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedAuthor
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDescriptionDto
import com.yandex.avatargenerator.feature.profile.domain.model.ProfileDetails
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * DTO выше `data/` не поднимаются: маппинг живёт здесь.
 *
 * Контракт `GET /users/{id}` беднее макета: `bio`, `avatar` и отдельного
 * `handle` в нём нет. До появления полей на бэкенде подставляем пустые дефолты
 * (`bio = ""`, `avatarUrl = null`, `handle = name`), а не выдумываем значения.
 */
internal fun UserProfileResponseDto.toDomain(userId: String): ProfileDetails {
    val id = id ?: userId
    val displayName = name.orEmpty()
    val profile = UserProfile(
        id = id,
        displayName = displayName,
        handle = displayName,
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
        publishedAt = createdAt?.toInstantOrNull() ?: Instant.EPOCH,
    )
}

/**
 * Разбирает `created_at`. `Instant.parse` берёт формы с `Z` и с офсетом
 * (`+03:00`); дату без зоны (`2026-08-14T10:00:00`) трактуем как UTC. Кривой
 * формат не роняет разбор — вернём `null`.
 */
private fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }
        .recoverCatching { LocalDateTime.parse(this).toInstant(ZoneOffset.UTC) }
        .getOrNull()
