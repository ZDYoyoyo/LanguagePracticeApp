package io.github.zdyoyoyo.englishpractice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
val FavoriteGold = Color(0xFFF9A825)

/**
 * @param darkTheme null = 跟隨系統
 * @param fontScale 額外的字體縮放（乘上系統設定的字體大小）
 */
@Composable
fun EnglishPracticeTheme(darkTheme: Boolean? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
    val dark = darkTheme ?: isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
