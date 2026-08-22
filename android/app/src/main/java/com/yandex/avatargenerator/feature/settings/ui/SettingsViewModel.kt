package com.yandex.avatargenerator.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.BuildConfig
import com.yandex.avatargenerator.feature.auth.domain.CurrentUserUseCase
import com.yandex.avatargenerator.feature.auth.domain.LogoutUseCase
import com.yandex.avatargenerator.feature.settings.domain.SettingsRepository
import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel экрана настроек: сводит тему, язык и данные текущей сессии в
 * [SettingsUiState], пишет выбор темы/языка в [SettingsRepository] и разлогинивает.
 *
 * Разлогин — разовое [SettingsEvent] через [events] (навигация уводит на вход).
 * Смена темы/языка события не требует: применяется вживую на корне Compose
 * (см. `MainActivity`), без пересоздания Activity.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val logoutUseCase: LogoutUseCase,
    currentUserUseCase: CurrentUserUseCase,
) : ViewModel() {

    // Channel(BUFFERED), а не SharedFlow: одноразовое событие не теряется, если
    // экран в этот момент не STARTED (фон/поворот во время logout/смены языка).
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.language,
        currentUserUseCase(),
    ) { themeMode, language, user ->
        SettingsUiState(
            nickname = user?.nickname.orEmpty(),
            email = user?.email.orEmpty(),
            themeMode = themeMode,
            language = language,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // Тема/язык — из синхронного кэша, а не SYSTEM: иначе селектор при открытии
        // мигнул бы «Системная», пока combine асинхронно не подтянет сохранённый выбор.
        initialValue = SettingsUiState.Initial.copy(
            themeMode = settingsRepository.lastThemeMode,
            language = settingsRepository.lastLanguage,
        ),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        // Язык применяется вживую (подмена локали на корне Compose) — пересоздания и
        // события не требуется.
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _events.send(SettingsEvent.LoggedOut)
        }
    }
}
