package com.yandex.avatargenerator.feature.subscription.data

import com.yandex.avatargenerator.feature.subscription.data.dto.SubscriptionUserDto
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/**
 * DTO выше `data/` не поднимаются: маппинг живёт здесь. Битый элемент (без `id`)
 * пропускаем, а не роняем весь список.
 *
 * `followersCount` оставляем `null`, если бэкенд его не прислал: контракт списка
 * счётчик не возвращает (см. `swagger.yml`), и «0 followers» у всех было бы
 * враньём — подпись просто не рисуем. `is_subscribed` отсутствует → считаем, что
 * не подписаны (кнопка Follow).
 */
internal fun List<SubscriptionUserDto>.toDomain(): List<SubscriptionUser> =
    mapNotNull { it.toDomainOrNull() }

private fun SubscriptionUserDto.toDomainOrNull(): SubscriptionUser? {
    val id = id ?: return null
    return SubscriptionUser(
        id = id,
        displayName = name.orEmpty(),
        avatarUrl = avatarUrl,
        followersCount = subscribersCount,
        isFollowing = isSubscribed ?: false,
    )
}
