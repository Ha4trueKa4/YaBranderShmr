package com.yandex.avatargenerator.feature.feed.di

import com.yandex.avatargenerator.feature.feed.data.FeedRepository
import com.yandex.avatargenerator.feature.feed.data.NetworkFeedRepository
import com.yandex.avatargenerator.feature.feed.ui.FeedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val feedModule = module {
    single<FeedRepository> { NetworkFeedRepository(get()) }
    viewModelOf(::FeedViewModel)
}
