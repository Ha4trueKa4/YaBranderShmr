package com.yandex.avatargenerator.feature.subscription.di

import com.yandex.avatargenerator.feature.subscription.data.NetworkSubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.ui.SubscriptionsTab
import com.yandex.avatargenerator.feature.subscription.ui.SubscriptionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subscriptionModule = module {
    single<SubscriptionRepository> { NetworkSubscriptionRepository(get()) }
    viewModel { (userId: String, initialTab: SubscriptionsTab) ->
        SubscriptionsViewModel(get(), userId, initialTab)
    }
}
