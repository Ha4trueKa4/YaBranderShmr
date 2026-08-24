package com.yandex.avatargenerator.feature.createproject.di

import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Своего репозитория у фичи нет: создание проекта — это `POST /projects`,
 * то есть метод `ProjectRepository` из `feature/project` (он же
 * зарегистрирован в `projectModule`); оттуда же берётся
 * `GenerationModelRepository` для выбора модели. Koin резолвит оба через общий
 * граф.
 */
val createProjectModule = module {
    viewModelOf(::CreateProjectViewModel)
}
