package com.yandex.avatargenerator.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository

/**
 * Ручная фабрика ViewModel (пока без DI). Когда в проект заведут Koin
 * (см. ARCHITECTURE.md §DI), это заменится на регистрацию ViewModel в
 * Koin-модуле фичи.
 */
class ProfileViewModelFactory(
    private val repository: ProfileRepository,
    private val userId: String,
    private val isOwnProfile: Boolean,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ProfileViewModel(repository, userId, isOwnProfile) as T
    }
}
