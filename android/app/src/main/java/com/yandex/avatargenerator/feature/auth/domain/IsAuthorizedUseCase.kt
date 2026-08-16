package com.yandex.avatargenerator.feature.auth.domain

import kotlinx.coroutines.flow.Flow

/** Есть ли действующая сессия — навигация выбирает по этому флагу стартовый экран. */
internal class IsAuthorizedUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.isAuthorized
}
