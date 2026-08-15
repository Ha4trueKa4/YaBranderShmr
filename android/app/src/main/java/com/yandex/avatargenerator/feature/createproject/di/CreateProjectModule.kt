package com.yandex.avatargenerator.feature.createproject.di

import com.yandex.avatargenerator.feature.createproject.data.MockCreateProjectRepository
import com.yandex.avatargenerator.feature.createproject.domain.CreateProjectRepository
import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val createProjectModule = module {
    single<CreateProjectRepository> { MockCreateProjectRepository() }
    viewModelOf(::CreateProjectViewModel)
}
