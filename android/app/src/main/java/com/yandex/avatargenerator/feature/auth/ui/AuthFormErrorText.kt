package com.yandex.avatargenerator.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yandex.avatargenerator.R

/**
 * Единственное место, где [AuthFormError] превращается в текст — общее для
 * экранов входа и регистрации, чтобы формулировки не разъехались.
 *
 * Тексты про длину пароля и никнейма повторяют правила бэкенда **в подсказке,
 * но не в логике**: проверок на клиенте нет, валидность решает сервер.
 */
@Composable
internal fun AuthFormError.text(): String = stringResource(
    when (this) {
        AuthFormError.InvalidCredentials -> R.string.auth_error_invalid_credentials
        AuthFormError.EmailTaken -> R.string.auth_error_email_taken
        AuthFormError.NicknameTaken -> R.string.auth_error_nickname_taken
        AuthFormError.InvalidEmail -> R.string.auth_error_invalid_email
        AuthFormError.InvalidPassword -> R.string.auth_error_invalid_password
        AuthFormError.InvalidNickname -> R.string.auth_error_invalid_nickname
        AuthFormError.YandexEmailTaken -> R.string.auth_error_yandex_email_taken
        AuthFormError.YandexFailed -> R.string.auth_error_yandex_failed
        AuthFormError.NoConnection -> R.string.auth_error_no_connection
        AuthFormError.ServerUnavailable -> R.string.auth_error_server
        AuthFormError.Unknown -> R.string.auth_error_unknown
    },
)
