package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** Текущий пользователь — вкладка профиля берёт отсюда свой id, настройки — email/никнейм. */
class CurrentUserUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.currentUser
}
