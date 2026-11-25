package io.github.zdyoyoyo.englishpractice.data.prefs

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 外觀設定：主題（淺色／深色）與字體大小。只是個人偏好，不備份到雲端 */
class DisplaySettings(context: Context) {
    enum class ThemeMode(val key: String, val label: String) {
        SYSTEM("system", "跟隨系統"), LIGHT("light", "淺色"), DARK("dark", "深色");
        companion object { fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: SYSTEM }
    }

    enum class FontSize(val key: String, val label: String, val scale: Float) {
        SMALL("s", "小", 0.9f), NORMAL("m", "標準", 1.0f), LARGE("l", "大", 1.15f), XLARGE("xl", "特大", 1.3f);
        companion object { fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: NORMAL }
    }

    private val sp = context.getSharedPreferences("display", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(ThemeMode.fromKey(sp.getString(KEY_THEME, null)))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()
    private val _font = MutableStateFlow(FontSize.fromKey(sp.getString(KEY_FONT, null)))
    val font: StateFlow<FontSize> = _font.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        sp.edit { putString(KEY_THEME, mode.key) }
        _theme.value = mode
    }

    fun setFont(size: FontSize) {
        sp.edit { putString(KEY_FONT, size.key) }
        _font.value = size
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_FONT = "font"
    }
}
