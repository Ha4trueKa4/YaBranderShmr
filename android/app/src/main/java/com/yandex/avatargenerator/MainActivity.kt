package com.yandex.avatargenerator

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.settings.data.SettingsPreferences
import com.yandex.avatargenerator.feature.settings.domain.SettingsRepository
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import com.yandex.avatargenerator.navigation.AvatarGeneratorApp
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.android.ext.android.inject
import java.util.Locale

// Значения scrim из рекомендаций androidx.activity для enableEdgeToEdge: почти
// непрозрачные белый/тёмный, чтобы кнопки навигации читались на любом контенте.
private val LightNavigationBarScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    /**
     * Применяет выбранную локаль к ресурсам ещё до создания Compose-иерархии.
     * Пустой тег — «как в системе», без переопределения. Смена языка в настройках
     * пересоздаёт Activity, поэтому здесь всегда читается актуальное значение.
     */
    override fun attachBaseContext(newBase: Context) {
        val tag = SettingsPreferences.currentLanguageTagBlocking(newBase)
        if (tag.isBlank()) {
            // «Как в системе»: возвращаем JVM-локаль по умолчанию к системной, иначе
            // после Русский → Системный в процессе осталась бы застрявшая ru
            // (процесс не перезапускается, только пересоздаётся Activity).
            Locale.setDefault(newBase.resources.configuration.locales.get(0))
            super.attachBaseContext(newBase)
        } else {
            super.attachBaseContext(newBase.withLocaleTag(tag))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // auto(...) сам подбирает светлые/тёмные иконки системных баров под активную
        // тему. Статус-бар оставляем полностью прозрачным — под ним контент экрана.
        // Навбару в 3-кнопочном режиме нужен запасной scrim: contrast enforcement
        // появился только на API 29+, а на 26–28 тёмные иконки светлой темы утонули бы
        // в тёмном контенте под баром (hero-градиент, превью проекта).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(LightNavigationBarScrim, DarkNavigationBarScrim),
        )
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            AvatarGeneratorTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                AvatarGeneratorApp()
            }
        }
    }
}

private fun Context.withLocaleTag(tag: String): Context {
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
