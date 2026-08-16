package com.yandex.avatargenerator.feature.profile.di

import com.yandex.avatargenerator.feature.profile.data.MockProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { MockProfileRepository() }
    viewModel { (userId: String, isOwnProfile: Boolean) ->
        ProfileViewModel(get(), userId, isOwnProfile)
    }
}
