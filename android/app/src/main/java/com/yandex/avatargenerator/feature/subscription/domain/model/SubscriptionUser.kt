package com.yandex.avatargenerator.feature.subscription.domain.model

/**
 * Пользователь в списке подписчиков/подписок. Строка на экране подписок:
 * аватар, имя, число подписчиков и кнопка Follow/Unfollow. По тапу по строке
 * открывается его профиль ([id]).
 *
 * @property avatarUrl аватар; `null` — круг-плейсхолдер с инициалами.
 * @property followersCount число подписчиков этого пользователя (подпись строки).
 *   `null`, если бэкенд его не отдаёт — контракт списка (`GET
 *   /users/{id}/subscribers`) счётчик не возвращает, только `id`/`name`/
 *   `is_subscribed` (см. `swagger.yml`). Тогда подпись не рисуем, а не показываем
 *   «0 followers».
 * @property isFollowing подписан ли текущий пользователь на него (состояние
 *   кнопки Follow/Unfollow).
 */
data class SubscriptionUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val followersCount: Int?,
    val isFollowing: Boolean,
)
