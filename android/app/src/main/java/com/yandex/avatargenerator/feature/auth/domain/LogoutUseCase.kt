package com.yandex.avatargenerator.feature.auth.domain

/** Локальный разлогин: чистит токен и сохранённый профиль (см. [AuthRepository.logout]). */
class LogoutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.logout()
}
