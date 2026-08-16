package com.yandex.avatargenerator.feature.createproject.di

import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Своего репозитория у фичи нет: создание проекта — это `POST /projects`,
 * то есть метод `ProjectRepository` из `feature/project` (он же
 * зарегистрирован в `projectModule`). Koin резолвит его через общий граф.
 */
val createProjectModule = module {
    viewModelOf(::CreateProjectViewModel)
}
