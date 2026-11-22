package io.github.zdyoyoyo.englishpractice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF2E6BE6),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFF57C00),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9DB9FF),
    secondary = Color(0xFF64D8CB),
    tertiary = Color(0xFFFFB74D),
)

val CorrectGreen = Color(0xFF2E7D32)
val WrongRed = Color(0xFFC62828)

@Composable
fun EnglishPracticeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
