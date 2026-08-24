package com.yandex.avatargenerator

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.yandex.avatargenerator.core.image.RepairingSvgDecoder
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

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                // Превью проектов приходят SVG (последняя версия из S3) — без
                // этого декодера Coil качает файл, но не рисует его. Обёртка
                // вместо голого SvgDecoder — чтобы битая разметка от генератора
                // не оставляла карточку без картинки.
                add(RepairingSvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            // Иначе провал декодирования виден только как серая плашка.
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
}
