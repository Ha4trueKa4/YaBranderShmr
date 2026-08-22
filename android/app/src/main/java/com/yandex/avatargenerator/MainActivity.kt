package com.yandex.avatargenerator

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.feature.settings.domain.SettingsRepository
import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.navigation.AvatarGeneratorApp
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.android.ext.android.inject
import java.util.Locale

// Значения scrim из рекомендаций androidx.activity для enableEdgeToEdge: почти
// непрозрачные белый/тёмный, чтобы кнопки навигации читались на любом контенте.
private val LightNavigationBarScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

/**
 * Тема и язык применяются вживую на корне Compose — без пересоздания Activity, чтобы
 * смена не мигала. Язык подменяет локаль через [ProvideAppLocale]; ресурсы неактивной
 * локали должны целиком лежать в base APK (`bundle { language { enableSplit = false } }`).
 */
class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

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
            // initialValue — последние известные тема/язык, а не дефолт: иначе первый
            // кадр мигал бы системными значениями, пока DataStore читается асинхронно.
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = settingsRepository.lastThemeMode)
            val language by settingsRepository.language
                .collectAsStateWithLifecycle(initialValue = settingsRepository.lastLanguage)
            AvatarGeneratorTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                ProvideAppLocale(language) {
                    AvatarGeneratorApp()
                }
            }
        }
    }
}

/**
 * Подменяет локаль для всего Compose-поддерева: `stringResource`/`pluralStringResource`
 * читают ресурсы из [LocalContext], поэтому даём контекст с нужной локалью. Смена
 * [language] пересобирает контекст и перерисовывает тексты на месте — без recreate.
 * [AppLanguage.SYSTEM] ничего не переопределяет (берётся системная локаль).
 *
 * `AppBundleLocaleChanges` подавлен осознанно: релизную корректность обеспечивает
 * `enableSplit = false` (все локали в base APK), Play Core-загрузка языков не нужна.
 */
@SuppressLint("AppBundleLocaleChanges")
@Composable
private fun ProvideAppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    if (language.tag.isEmpty()) {
        content()
        return
    }
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(language, baseConfiguration) {
        val configuration = Configuration(baseConfiguration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
        baseContext.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content,
    )
}
