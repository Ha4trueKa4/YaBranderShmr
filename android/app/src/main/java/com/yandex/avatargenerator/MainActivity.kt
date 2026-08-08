package com.yandex.avatargenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.yandex.avatargenerator.feature.feed.ui.FeedScreen
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvatarGeneratorTheme {
                // Временный хост: показываем Feed напрямую, пока нет навигационного
                // скелета (bottom bar + NavHost — зона задачи онбординга/навигации).
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FeedScreen(
                        onProjectClick = { /* TODO: навигация на экран проекта */ },
                        onAuthorClick = { /* TODO: навигация на профиль автора */ },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}
