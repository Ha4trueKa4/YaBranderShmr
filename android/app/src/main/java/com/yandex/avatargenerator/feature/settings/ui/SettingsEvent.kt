package com.yandex.avatargenerator.feature.settings.ui

/** Разовые события экрана настроек, требующие реакции навигации/Activity. */
sealed interface SettingsEvent {

    /** Сессия завершена — уводим на экран входа. */
    data object LoggedOut : SettingsEvent

    /** Язык сохранён — пересоздаём Activity, чтобы ресурсы перечитались в новой локали. */
    data object LanguageChanged : SettingsEvent
}
