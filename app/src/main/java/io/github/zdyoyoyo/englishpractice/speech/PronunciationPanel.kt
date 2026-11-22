package io.github.zdyoyoyo.englishpractice.speech

import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.github.zdyoyoyo.englishpractice.domain.PronunciationChecker
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

/** 「🔊 聽發音 / 🐢 慢速 / 🎤 跟著唸」區塊 */
@Composable
fun PronunciationPanel(text: String, pronouncer: Pronouncer) {
    var result by remember(text) { mutableStateOf<Pair<Boolean, String>?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val heard = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        result = if (PronunciationChecker.matches(text, heard)) {
            true to "👍 發音正確！"
        } else {
            false to "聽起來像「${heard.firstOrNull() ?: "（沒聽清楚）"}」，再試一次？"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("發音練習：$text", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                if (!pronouncer.speak(text)) result = false to "這支手機沒有英文語音，請到系統設定安裝「文字轉語音」英文語音資料"
            }) { Text("🔊 聽") }
            OutlinedButton(onClick = { pronouncer.speak(text, slow = true) }) { Text("🐢 慢速") }
            OutlinedButton(onClick = {
                try {
                    launcher.launch(Pronouncer.recognizeIntent("請唸：$text"))
                } catch (e: ActivityNotFoundException) {
                    result = false to "這支手機沒有語音辨識服務"
                }
            }) { Text("🎤 跟著唸") }
        }
        result?.let { (ok, msg) -> Text(msg, color = if (ok) CorrectGreen else WrongRed) }
    }
}
