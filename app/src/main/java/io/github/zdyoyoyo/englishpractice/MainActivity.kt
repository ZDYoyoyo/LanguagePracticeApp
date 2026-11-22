package io.github.zdyoyoyo.englishpractice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.zdyoyoyo.englishpractice.ui.navigation.AppNavHost
import io.github.zdyoyoyo.englishpractice.ui.theme.EnglishPracticeTheme

/** 單一 Activity，所有畫面都是 Compose 畫面，用 Navigation 切換 */
class MainActivity : ComponentActivity() {

    /** 從通知點進來時要開啟的畫面（例如每日一題） */
    private val pendingOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingOpen.value = intent?.getStringExtra(EXTRA_OPEN)
        setContent {
            EnglishPracticeTheme {
                AppNavHost(
                    pendingOpen = pendingOpen.value,
                    onPendingOpenHandled = { pendingOpen.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen.value = intent.getStringExtra(EXTRA_OPEN)
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_DAILY = "daily"
    }
}
