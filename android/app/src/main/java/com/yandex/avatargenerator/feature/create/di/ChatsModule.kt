package com.yandex.avatargenerator.feature.create.di

import com.yandex.avatargenerator.feature.create.data.MockChatsRepository
import com.yandex.avatargenerator.feature.create.domain.ChatsRepository
import com.yandex.avatargenerator.feature.create.ui.ChatsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatsModule = module {
    single<ChatsRepository> { MockChatsRepository() }
    viewModelOf(::ChatsViewModel)
}
