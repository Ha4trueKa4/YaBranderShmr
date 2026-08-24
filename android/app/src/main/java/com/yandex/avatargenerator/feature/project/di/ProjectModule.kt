package com.yandex.avatargenerator.feature.project.di

import com.yandex.avatargenerator.feature.project.data.AndroidProjectExporter
import com.yandex.avatargenerator.feature.project.data.ChatRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.NetworkPublicProjectRepository
import com.yandex.avatargenerator.feature.project.data.ProjectRepositoryImpl
import com.yandex.avatargenerator.feature.project.data.VersionsRepositoryImpl
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectExporter
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.ui.ProjectViewModel
import com.yandex.avatargenerator.feature.project.ui.publicproject.PublicProjectViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val projectModule = module {
    single<ChatRepository> {
        ChatRepositoryImpl(
            client = get(),
            projectRepository = get(),
            versionsRepository = get(),
        )
    }
    single<ProjectRepository> { ProjectRepositoryImpl(client = get()) }
    single<PublicProjectRepository> { NetworkPublicProjectRepository(get()) }
    single<VersionsRepository> { VersionsRepositoryImpl(client = get()) }
    single<ProjectExporter> { AndroidProjectExporter(androidContext(), get()) }
    viewModel { (projectId: String) -> ProjectViewModel(get(), get(), get(), projectId) }
    viewModel { (projectId: String) -> PublicProjectViewModel(get(), get(), get(), projectId) }
}
