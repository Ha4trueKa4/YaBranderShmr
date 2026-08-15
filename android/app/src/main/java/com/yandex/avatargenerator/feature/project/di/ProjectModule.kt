package com.yandex.avatargenerator.feature.project.di

import com.yandex.avatargenerator.feature.project.data.MockChatRepository
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.ui.ProjectViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val projectModule = module {
    single<ChatRepository> { MockChatRepository() }
    // projectId известен только в момент открытия экрана — передаём его
    // параметром (parametersOf в koinViewModel), а не резолвим из графа.
    viewModel { (projectId: String) -> ProjectViewModel(get(), projectId) }
}
