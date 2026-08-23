package com.yandex.avatargenerator.feature.create.di

import com.yandex.avatargenerator.feature.create.data.ChatsRepositoryImpl
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.ui.ChatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule = module {
    single<ChatsRepository> { ChatsRepositoryImpl(get(), get()) }
    // Не viewModelOf: *Of-DSL резолвил бы через get() и nowMillis, а его в графе нет.
    viewModel { ChatsViewModel(get()) }
}
