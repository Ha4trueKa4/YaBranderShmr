package com.yandex.avatargenerator.feature.profile.di

import com.yandex.avatargenerator.feature.profile.data.NetworkProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { NetworkProfileRepository(get()) }
    viewModel { (userId: String, isOwnProfile: Boolean) ->
        ProfileViewModel(get(), get(), userId, isOwnProfile)
    }
}
