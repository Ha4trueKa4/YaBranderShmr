package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.data.toInstantOrNull
import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.dto.ProjectDescriptionDto
import com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectAuthor
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectRelatedItem

/**
 * Собирает витрину публичного проекта из двух ответов: `GET /projects/{id}` (сам
 * проект) и `GET /users/{author_id}` (имя автора, его подписка и работы для
 * «More from this creator»).
 *
 * [author] опционален: если ручка профиля не ответила, проект всё равно
 * показываем — без имени автора и подборки. Контракт беднее макета — `badge` и
 * `role` в нём нет, поэтому подставляем пустые дефолты (UI прячет пустую плашку
 * и роль), а не выдумываем значения. Превью — SVG-`url` из S3 (Coil рисует его
 * через `SvgDecoder`); у пустого проекта `url` нет — тогда hero-градиент.
 */
internal fun ProjectDto.toPublicProject(author: UserProfileResponseDto?): PublicProject {
    val projectId = id.orMalformed("id")
    return PublicProject(
        id = projectId,
        title = name.orMalformed("name"),
        badge = "",
        previewUrl = url,
        publishedAt = createdAt?.toInstantOrNull(),
        author = PublicProjectAuthor(
            id = author?.id ?: authorId.orEmpty(),
            name = author?.name.orEmpty(),
            role = "",
            avatarUrl = null,
            isFollowing = author?.isSubscribed ?: false,
        ),
        // Текущий проект из подборки исключаем — он и так открыт.
        related = author?.projects.orEmpty()
            .filter { it.id != null && it.id != projectId }
            .map { it.toRelatedItem() },
    )
}

private fun ProjectDescriptionDto.toRelatedItem(): PublicProjectRelatedItem =
    PublicProjectRelatedItem(
        id = id.orEmpty(),
        title = name.orEmpty(),
        subtitle = description.orEmpty(),
        previewUrl = url,
    )

/** Пустое обязательное поле — это сломанный контракт, а не «нет значения». */
private fun String?.orMalformed(field: String): String =
    this ?: throw NetworkException.Serialization(
        IllegalStateException("Project.$field is missing in response"),
    )
