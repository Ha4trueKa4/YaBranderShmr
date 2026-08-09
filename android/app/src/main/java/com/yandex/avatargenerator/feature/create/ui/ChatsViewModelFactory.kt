package com.yandex.avatargenerator.feature.create.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository

/**
 * Ручная фабрика ViewModel (пока без DI) — как [com.yandex.avatargenerator.feature.feed.ui.FeedViewModelFactory].
 * Подключение общего DI-каркаса — отдельная задача на всё приложение.
 */
internal class ChatsViewModelFactory(
    private val repository: ChatsRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ChatsViewModel(repository) as T
    }
}
