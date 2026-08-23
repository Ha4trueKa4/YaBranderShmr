package com.yandex.avatargenerator

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
        applyEdgeToEdge(settingsRepository.lastThemeMode.isDark(resources.configuration.isNightMode()))
        setContent {
            // initialValue — последние известные тема/язык, а не дефолт: иначе первый
            // кадр мигал бы системными значениями, пока DataStore читается асинхронно.
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = settingsRepository.lastThemeMode)
            val language by settingsRepository.language
                .collectAsStateWithLifecycle(initialValue = settingsRepository.lastLanguage)
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            // Тема живёт в приложении, а не в uiMode системы, поэтому цвет иконок баров
            // переприменяем сами: иначе в светлой теме поверх тёмной системной остались
            // бы белые иконки на белом фоне.
            DisposableEffect(darkTheme) {
                applyEdgeToEdge(darkTheme)
                onDispose {}
            }
            AvatarGeneratorTheme(darkTheme = darkTheme) {
                ProvideAppLocale(language) {
                    AvatarGeneratorApp()
                }
            }
        }
    }

    /**
     * Статус-бар оставляем полностью прозрачным — под ним контент экрана. Навбару в
     * 3-кнопочном режиме нужен запасной scrim: contrast enforcement появился только на
     * API 29+, а на 26–28 тёмные иконки светлой темы утонули бы в тёмном контенте под
     * баром (hero-градиент, превью проекта).
     */
    private fun applyEdgeToEdge(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                LightNavigationBarScrim,
                DarkNavigationBarScrim,
            ) { darkTheme },
        )
    }
}

private fun Configuration.isNightMode(): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

/**
 * Подменяет локаль для всего Compose-поддерева: `stringResource`/`pluralStringResource`
 * читают ресурсы из [LocalContext], поэтому даём контекст с нужной локалью. Смена
 * [language] пересобирает контекст и перерисовывает тексты на месте — без recreate.
 * [AppLanguage.SYSTEM] ничего не переопределяет (берётся системная локаль).
 *
 * `AppBundleLocaleChanges` подавлен осознанно: релизную корректность обеспечивает
 * `enableSplit = false` (все локали в base APK), Play Core-загрузка языков не нужна.
 *
 * Вместе с контекстом переопределяем [LocalActivityResultRegistryOwner]: его дефолтный
 * резолвер ищет владельца, обходя `ContextWrapper`-цепочку от [LocalContext]
 * (`findOwner` в activity-compose), а подменённый `createConfigurationContext`-контекст к
 * Activity по этой цепочке не ведёт. Без явного проброса любой
 * `rememberLauncherForActivityResult` в поддереве (системные пикеры, запрос разрешений,
 * фото-пикер) падал бы с `IllegalStateException: No ActivityResultRegistryOwner...`.
 * Остальные owner'ы (Lifecycle/SavedState/OnBackPressedDispatcher) резолвятся через
 * `LocalView`/view-tree, а не через контекст, и подмену переживают — их не трогаем.
 */
@SuppressLint("AppBundleLocaleChanges")
@Composable
private fun ProvideAppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    // Захватываем владельца до подмены LocalContext, пока цепочка ещё ведёт к Activity.
    val activityResultRegistryOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "ProvideAppLocale must be hosted in a ComponentActivity"
    }
    // Контекст пересобираем только для явного языка; для SYSTEM (пустой тег) отдаём
    // базовый как есть. Важно, что content() при любом языке вызывается из одного и
    // того же CompositionLocalProvider: раньше пустой тег уходил в отдельную ветку с
    // прямым content(), и первая смена SYSTEM→язык переносила вызов в другое место
    // композиции — поддерево (вместе с NavHost и его navController) пересоздавалось,
    // сбрасывая навигацию на стартовый экран (условно, с настроек на ленту).
    val localizedContext = remember(language, baseConfiguration) {
        if (language.tag.isEmpty()) {
            baseContext
        } else {
            val configuration = Configuration(baseConfiguration).apply {
                setLocale(Locale.forLanguageTag(language.tag))
            }
            baseContext.createConfigurationContext(configuration)
        }
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        content = content,
    )
}
