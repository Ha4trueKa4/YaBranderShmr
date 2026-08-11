package com.yandex.avatargenerator.feature.profile.di

import com.yandex.avatargenerator.feature.profile.data.MockProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { MockProfileRepository() }
    // userId и isOwnProfile известны только в момент открытия экрана — передаём
    // их параметрами (parametersOf в koinViewModel), а не резолвим из графа.
    viewModel { (userId: String, isOwnProfile: Boolean) ->
        ProfileViewModel(get(), userId, isOwnProfile)
    }
}
