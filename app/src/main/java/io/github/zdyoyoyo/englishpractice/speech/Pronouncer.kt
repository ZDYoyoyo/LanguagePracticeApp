package io.github.zdyoyoyo.englishpractice.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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

    private var defaultVoice: Voice? = null
    private var maleVoice: Voice? = null
    private var femaleVoice: Voice? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isReady) findVoices()
        }
    }

    /** 找出手機上的英文男聲 / 女聲；找不到就只用音高區分 */
    private fun findVoices() {
        runCatching {
            defaultVoice = tts.voice
            val all = tts.voices.orEmpty().filter { !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            val candidates = all.map { VoicePicker.Candidate(it.name, it.locale.toLanguageTag(), it.isNetworkConnectionRequired) }
            fun find(g: VoicePicker.Gender) = VoicePicker.pick(candidates, g)?.let { c -> all.first { it.name == c.name } }
            maleVoice = find(VoicePicker.Gender.MALE)
            femaleVoice = find(VoicePicker.Gender.FEMALE)
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
            // 有找到真正的男聲 / 女聲就切換聲音（音高只微調）；找不到就用較大的音高差異
            val voice = when (line.pitch) {
                DialogueScript.MALE_PITCH -> maleVoice
                DialogueScript.FEMALE_PITCH -> femaleVoice
                else -> null
            }
            tts.voice = voice ?: defaultVoice ?: tts.voice
            tts.setPitch(
                when {
                    voice != null && line.pitch == DialogueScript.MALE_PITCH -> 0.95f
                    voice != null -> 1.05f
                    line.pitch == DialogueScript.MALE_PITCH -> 0.7f   // 沒有男聲時：明顯壓低
                    else -> line.pitch
                }
            )
            tts.speak(line.text, TextToSpeech.QUEUE_ADD, null, "line$i")
            tts.playSilentUtterance(350, TextToSpeech.QUEUE_ADD, "pause$i")
        }
        tts.setPitch(1.0f)
        defaultVoice?.let { tts.voice = it }
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
