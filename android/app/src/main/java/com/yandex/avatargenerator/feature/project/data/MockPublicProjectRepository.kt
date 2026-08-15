package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectAuthor
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectRelatedItem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Временный мок публичного проекта для разработки UI до готовности бэкенда:
 * отдаёт данные из макета (автор, подборка работ) и имитирует сетевую задержку.
 *
 * `GET /projects/{id}` пока не возвращает ни автора, ни подборку, поэтому экран
 * живёт на моке; при появлении ручек добавится сетевая реализация.
 *
 * @param networkDelayMillis имитация задержки загрузки — чтобы видеть Loading.
 */
class MockPublicProjectRepository(
    private val networkDelayMillis: Long = 600L,
) : PublicProjectRepository {

    override suspend fun getPublicProject(projectId: String): Result<PublicProject> {
        delay(networkDelayMillis)
        return Result.success(sample(projectId))
    }

    companion object {

        /** Проект ровно по макету экрана публичного проекта. */
        fun sample(id: String = "public-sample", now: Instant = Instant.now()): PublicProject =
            PublicProject(
                id = id,
                title = "Nova Studio Brand",
                badge = "AI Identity v4",
                previewUrl = null,
                publishedAt = now.minus(3, ChronoUnit.DAYS),
                author = PublicProjectAuthor(
                    id = "author-sarah",
                    name = "Sarah Chen",
                    role = "Product Designer",
                    avatarUrl = null,
                    isFollowing = false,
                ),
                related = listOf(
                    PublicProjectRelatedItem("p1", "Vortex Logo", "Brand Asset", null),
                    PublicProjectRelatedItem("p2", "Aero Pattern", "Brand Asset", null),
                    PublicProjectRelatedItem("p3", "Lumina Icon", "Brand Asset", null),
                    PublicProjectRelatedItem("p4", "Helix Brand", "Brand Asset", null),
                ),
            )
    }
}
