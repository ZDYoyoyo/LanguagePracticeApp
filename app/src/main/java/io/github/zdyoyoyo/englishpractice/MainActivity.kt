package io.github.zdyoyoyo.englishpractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.zdyoyoyo.englishpractice.ui.navigation.AppNavHost
import io.github.zdyoyoyo.englishpractice.ui.theme.EnglishPracticeTheme

/** 單一 Activity，所有畫面都是 Compose 畫面，用 Navigation 切換 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishPracticeTheme {
                AppNavHost()
            }
        }
    }
}
