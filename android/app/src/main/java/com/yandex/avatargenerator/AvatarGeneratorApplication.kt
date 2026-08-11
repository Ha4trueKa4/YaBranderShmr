package com.yandex.avatargenerator

import android.app.Application
import com.yandex.avatargenerator.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Точка старта Koin: граф собирается один раз на процесс, до первого экрана.
 * ViewModel и репозитории берутся из [appModules], ручных фабрик/провайдеров нет.
 */
class AvatarGeneratorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AvatarGeneratorApplication)
            modules(appModules)
        }
    }
}
