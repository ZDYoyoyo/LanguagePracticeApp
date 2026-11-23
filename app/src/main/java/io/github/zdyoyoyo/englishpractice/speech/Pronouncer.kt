package io.github.zdyoyoyo.englishpractice.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * 朗讀英文：使用 Android 內建的 TextToSpeech（不需要任何權限，裝了語音資料就能離線使用）。
 */
class Pronouncer(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile var isReady = false
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    /** @return false 代表手機沒有英文語音引擎 */
    fun speak(text: String, slow: Boolean = false): Boolean {
        if (!isReady) return false
        tts.setSpeechRate(if (slow) 0.6f else 0.95f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
        return true
    }

    /**
     * 播放聽力對話：每行開頭的 M: / W: 代表男聲 / 女聲，用不同音高區分（標籤本身不唸出來）。
     * @return false 代表手機沒有英文語音引擎
     */
    fun speakDialogue(script: String, slow: Boolean = false): Boolean {
        if (!isReady) return false
        tts.setSpeechRate(if (slow) 0.7f else 0.95f)
        tts.stop()
        DialogueScript.parse(script).forEachIndexed { i, line ->
            tts.setPitch(line.pitch)
            tts.speak(line.text, TextToSpeech.QUEUE_ADD, null, "line$i")
            tts.playSilentUtterance(350, TextToSpeech.QUEUE_ADD, "pause$i")
        }
        tts.setPitch(1.0f)
        return true
    }

    fun stop() = tts.stop()

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        /**
         * 跟著唸：用系統的語音辨識畫面（RecognizerIntent）。
         * 由系統（通常是 Google）處理錄音，本 App 不需要麥克風權限、也不會拿到錄音檔，只拿到辨識出的文字。
         */
        fun recognizeIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
}

/** 在 Compose 畫面中取得一個 Pronouncer，離開畫面時自動釋放 */
@Composable
fun rememberPronouncer(): Pronouncer {
    val context = LocalContext.current
    val pronouncer = remember { Pronouncer(context) }
    DisposableEffect(pronouncer) { onDispose { pronouncer.shutdown() } }
    return pronouncer
}
