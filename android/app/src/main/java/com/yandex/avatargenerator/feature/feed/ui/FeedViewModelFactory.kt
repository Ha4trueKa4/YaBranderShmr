package com.yandex.avatargenerator.feature.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yandex.avatargenerator.feature.feed.data.FeedRepository

/**
 * Ручная фабрика ViewModel (пока без DI). Когда в проект добавят Hilt,
 * это заменится на `@HiltViewModel` + конструкторную инъекцию.
 */
class FeedViewModelFactory(
    private val repository: FeedRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return FeedViewModel(repository) as T
    }
}
