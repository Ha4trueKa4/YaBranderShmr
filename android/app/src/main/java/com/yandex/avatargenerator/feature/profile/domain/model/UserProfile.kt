package com.yandex.avatargenerator.feature.profile.domain.model

/**
 * Профиль пользователя (свой или чужой) по макету.
 *
 * @property handle никнейм без «@» (собаку добавляет UI); `null`, если бэкенд
 *   его не отдаёт — тогда строку никнейма не показываем (не подставляем имя).
 * @property bio краткое описание, показывается по центру под именем.
 * @property avatarUrl аватар; null — круг-плейсхолдер.
 * @property isFollowing подписан ли текущий пользователь на этого (для чужого
 *   профиля управляет состоянием кнопки Follow/Following; для своего игнорируется).
 */
data class UserProfile(
    val id: String,
    val displayName: String,
    val handle: String?,
    val bio: String,
    val avatarUrl: String?,
    val projectsCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
)
