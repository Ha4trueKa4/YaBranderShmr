package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** Текущий пользователь — отсюда вкладка профиля берёт свой id. */
internal class CurrentUserUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = repository.currentUser
}
