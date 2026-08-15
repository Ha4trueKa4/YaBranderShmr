package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.auth.domain.IsAuthorizedUseCase
import org.koin.compose.koinInject

/**
 * Есть ли действующая сессия — нужно навигации, чтобы выбрать стартовый экран.
 *
 * `null` означает «токен ещё читается из хранилища». Чтение асинхронное, и на
 * первом кадре ответа нет; вызывающий обязан дождаться не-null, иначе стартовый
 * экран выберется наугад и авторизованному пользователю мигнёт экран входа.
 */
@Composable
internal fun rememberIsAuthorized(): Boolean? {
    val isAuthorized = koinInject<IsAuthorizedUseCase>()
    return isAuthorized().collectAsStateWithLifecycle(initialValue = null).value
}
