package com.yandex.avatargenerator.feature.project.di

import com.yandex.avatargenerator.feature.project.data.MockChatRepository
import com.yandex.avatargenerator.feature.project.data.MockPublicProjectRepository
import com.yandex.avatargenerator.feature.project.data.ProjectRepositoryImpl
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.ui.ProjectViewModel
import com.yandex.avatargenerator.feature.project.ui.PublicProjectViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val projectModule = module {
    single<ChatRepository> { MockChatRepository() }
    single<ProjectRepository> { ProjectRepositoryImpl(client = get()) }
    single<PublicProjectRepository> { MockPublicProjectRepository() }
    viewModel { (projectId: String) -> ProjectViewModel(get(), get(), projectId) }
    viewModel { (projectId: String) -> PublicProjectViewModel(get(), projectId) }
}
