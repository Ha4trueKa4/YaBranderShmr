package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession

/**
 * Вход/регистрация через Yandex ID. На вход — `code`, полученный экраном из
 * редиректа страницы согласия; бэкенд меняет его на токен и заводит аккаунт,
 * если такого ещё не было. Токен репозиторий сохраняет сам.
 *
 * @throws AuthException.YandexEmailConflict email занят аккаунтом с паролем (409).
 * @throws AuthException.YandexAuthFailed Yandex ID не отдал профиль (400/502).
 */
internal class LoginWithYandexUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(code: String): AuthSession = repository.loginWithYandex(code)
}
