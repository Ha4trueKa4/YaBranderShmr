package com.yandex.avatargenerator.feature.auth.ui.register

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана регистрации — вся навигация отсюда. Отдаются через
 * канал, а не через [RegisterState], чтобы переход не повторялся на каждой
 * рекомпозиции.
 */
internal sealed interface RegisterEffect : BaseEffect {

    /** Аккаунт создан, токен сохранён — можно уходить в приложение. */
    data object NavigateToApp : RegisterEffect

    data object NavigateToLogin : RegisterEffect
}
