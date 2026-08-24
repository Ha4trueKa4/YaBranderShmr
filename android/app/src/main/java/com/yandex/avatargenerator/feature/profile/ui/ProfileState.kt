package com.yandex.avatargenerator.feature.profile.ui

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile

/**
 * Состояние экрана профиля: один immutable-класс со всеми флагами, как требует
 * [BaseState]. Раньше здесь была взаимоисключающая sealed-иерархия
 * `Loading/Error/Content`.
 *
 * @property profile загруженный профиль; `null`, пока его нет (загрузка или ошибка).
 * @property isOwnProfile свой профиль (кнопка «Edit profile») или чужой
 *  (кнопка «Follow»/«Following»). Известен при открытии экрана — приходит
 *  в конструктор ViewModel и с самого начала лежит в состоянии.
 * @property showBackButton рисовать ли шапку с «назад». Своя вкладка открыта из
 *  нижней панели, возвращаться там некуда, а чужой профиль — экран-деталь.
 * @property showSettingsButton рисовать ли шестерёнку — только на своём профиле.
 * @property isFollowInFlight запрос подписки в полёте; раньше это был приватный
 *  `followJob`, невидимый ни UI, ни тестам.
 */
data class ProfileState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val projects: List<FeedProject> = emptyList(),
    val hasError: Boolean = false,
    val isOwnProfile: Boolean = false,
    val showBackButton: Boolean = false,
    val showSettingsButton: Boolean = false,
    val isFollowInFlight: Boolean = false,
) : BaseState
