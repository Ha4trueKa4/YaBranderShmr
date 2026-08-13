package com.yandex.avatargenerator

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.yandex.avatargenerator.di.appModules
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * Точка старта Koin: граф собирается один раз на процесс, до первого экрана.
 * ViewModel и репозитории берутся из [appModules], ручных фабрик/провайдеров нет.
 */
class AvatarGeneratorApplication : Application(), SingletonImageLoader.Factory, KoinComponent {

    private val okHttpClient: OkHttpClient by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AvatarGeneratorApplication)
            modules(appModules)
        }
    }

    // Тот же клиент, что у Ktor — общий пул соединений и таймауты для API и картинок.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(okHttpClient)) }
            .build()
}
