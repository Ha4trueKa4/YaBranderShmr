package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.auth.domain.CurrentUserUseCase
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import org.koin.compose.koinInject

/**
 * Текущий пользователь — нужен вкладке профиля, чтобы знать свой id.
 *
 * `null` — либо ещё читается из хранилища, либо сессия заведена до появления
 * этого хранилища: различить нельзя, поэтому вызывающий обязан обработать `null`
 * как «id недоступен», а не ждать значения вечно.
 */
@Composable
internal fun rememberCurrentUser(): AuthUser? {
    val currentUser = koinInject<CurrentUserUseCase>()
    return currentUser().collectAsStateWithLifecycle(initialValue = null).value
}
