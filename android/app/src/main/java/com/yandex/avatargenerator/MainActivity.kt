package com.yandex.avatargenerator

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yandex.avatargenerator.navigation.AvatarGeneratorApp
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Прозрачный scrim для системных баров: в 3-кнопочном режиме по умолчанию
        // подкладывается непрозрачный scrim (на светлой теме — белая полоска снизу).
        // dark(...) убирает scrim и делает иконки системных кнопок светлыми — они
        // ложатся прямо на тёмную область нижней панели. Иконки статус-бара тоже
        // светлые (весь UI приложения тёмный).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AvatarGeneratorTheme {
                AvatarGeneratorApp()
            }
        }
    }
}
