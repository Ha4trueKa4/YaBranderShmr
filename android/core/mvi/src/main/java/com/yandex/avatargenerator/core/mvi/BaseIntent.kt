package com.yandex.avatargenerator.core.mvi

/**
 * Намерение пользователя (не `android.content.Intent`): «нажал кнопку»,
 * «изменил поле». Единственный способ что-либо сообщить [BaseViewModel].
 *
 * Наследники — `sealed interface` фичи, ветки которого именуются по действию
 * пользователя: `OnRetryClick`, `OnQueryChange(query)`.
 */
interface BaseIntent
