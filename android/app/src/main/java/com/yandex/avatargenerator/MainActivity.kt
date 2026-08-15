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
        // подкладывается непрозрачный. auto(...) убирает его и сам подбирает
        // светлые/тёмные иконки под активную тему.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AvatarGeneratorTheme {
                AvatarGeneratorApp()
            }
        }
    }
}
