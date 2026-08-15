package com.yandex.avatargenerator.feature.auth.ui

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.auth.domain.AuthException

/**
 * Ошибка формы входа/регистрации в виде значения, а не готового текста: маппинг
 * исключения проверяется юнит-тестом без Android, а состояние экрана не тянет `R`.
 * Текст подбирает [text] в `AuthFormErrorText.kt`.
 *
 * Сообщения сервера пользователю не показываем — они английские и написаны для
 * разработчика («Password must be 8-128 chars…»).
 */
internal enum class AuthFormError {
    /** `401` на входе: неверная пара email/пароль. */
    InvalidCredentials,

    /** `409 EMAIL_EXISTS`. */
    EmailTaken,

    /** `409 NICKNAME_EXISTS`. */
    NicknameTaken,

    /** `400 INVALID_EMAIL`. */
    InvalidEmail,

    /** `400 INVALID_PASSWORD`. */
    InvalidPassword,

    /** `400 INVALID_NICKNAME`. */
    InvalidNickname,

    /** Нет сети или таймаут. */
    NoConnection,

    /** `5xx`: бэкенд лёг. */
    ServerUnavailable,

    /** Классифицировать не удалось — показываем общее «что-то пошло не так». */
    Unknown,
}

/** Поле формы, под которым показать ошибку. */
internal enum class AuthField { Email, Password, Nickname }

/** Исключение репозитория → ошибка формы. Незнакомые коды сводим к [AuthFormError.Unknown]. */
internal fun Throwable.toAuthFormError(): AuthFormError = when (this) {
    is AuthException.InvalidCredentials -> AuthFormError.InvalidCredentials
    is AuthException.RegistrationConflict -> when (field) {
        AuthException.Field.Email -> AuthFormError.EmailTaken
        AuthException.Field.Nickname -> AuthFormError.NicknameTaken
        else -> AuthFormError.Unknown
    }

    is AuthException.RegistrationRejected -> when (field) {
        AuthException.Field.Email -> AuthFormError.InvalidEmail
        AuthException.Field.Password -> AuthFormError.InvalidPassword
        AuthException.Field.Nickname -> AuthFormError.InvalidNickname
        else -> AuthFormError.Unknown
    }

    is NetworkException.NoConnection, is NetworkException.Timeout -> AuthFormError.NoConnection
    is NetworkException.ServerError -> AuthFormError.ServerUnavailable
    else -> AuthFormError.Unknown
}

/** Поле, под которым показать ошибку; `null` — ошибка общая, показываем над кнопкой. */
internal fun AuthFormError.targetField(): AuthField? = when (this) {
    AuthFormError.EmailTaken, AuthFormError.InvalidEmail -> AuthField.Email
    AuthFormError.InvalidPassword -> AuthField.Password
    AuthFormError.NicknameTaken, AuthFormError.InvalidNickname -> AuthField.Nickname
    else -> null
}
