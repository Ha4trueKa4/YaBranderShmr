package com.yandex.avatargenerator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yandex.avatargenerator.navigation.AvatarGeneratorApp
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

// Значения scrim из рекомендаций androidx.activity для enableEdgeToEdge: почти
// непрозрачные белый/тёмный, чтобы кнопки навигации читались на любом контенте.
private val LightNavigationBarScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {
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
            AvatarGeneratorTheme {
                AvatarGeneratorApp()
            }
        }
    }
}
