package com.yandex.avatargenerator.feature.subscription.di

import com.yandex.avatargenerator.feature.subscription.data.NetworkSubscriptionRepository
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import org.koin.dsl.module

val subscriptionModule = module {
    single<SubscriptionRepository> { NetworkSubscriptionRepository(get()) }
}
