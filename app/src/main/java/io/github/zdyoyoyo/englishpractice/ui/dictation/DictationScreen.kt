package io.github.zdyoyoyo.englishpractice.ui.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.domain.DictationChecker
import io.github.zdyoyoyo.englishpractice.domain.DictationItem
import io.github.zdyoyoyo.englishpractice.domain.DictationSource
import io.github.zdyoyoyo.englishpractice.speech.rememberPronouncer
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictationUiState(
    val loading: Boolean = true,
    val items: List<DictationItem> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    val result: DictationChecker.Result? = null,
    val results: List<DictationChecker.Result> = emptyList(),
    val finished: Boolean = false,
) {
    val current: DictationItem? get() = items.getOrNull(index)
    val averageRatio: Double get() = if (results.isEmpty()) 0.0 else results.map { it.ratio }.average()
}

class DictationViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(DictationUiState())
    val ui: StateFlow<DictationUiState> = _ui.asStateFlow()

    init { restart() }

    fun restart() {
        viewModelScope.launch {
            val items = DictationSource.session(c.questionBank.ensureLoaded().questions)
            _ui.value = DictationUiState(loading = false, items = items)
        }
    }

    fun onInput(text: String) = _ui.update { it.copy(input = text) }

    fun check() {
        val s = _ui.value
        val item = s.current ?: return
        if (s.result != null || s.input.isBlank()) return
        val r = DictationChecker.check(item.text, s.input)
        _ui.update { it.copy(result = r, results = it.results + r) }
        // 聽寫不屬於題庫作答，但算進今天的打卡與每日目標
        viewModelScope.launch { c.study.recordActivity() }
    }

    fun next() {
        val s = _ui.value
        if (s.index >= s.items.lastIndex) _ui.update { it.copy(finished = true) }
        else _ui.update { it.copy(index = it.index + 1, input = "", result = null) }
    }

    companion object {
        val Factory = viewModelFactory { initializer { DictationViewModel(appContainer()) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DictationScreen(onBack: () -> Unit, vm: DictationViewModel = viewModel(factory = DictationViewModel.Factory)) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val pronouncer = rememberPronouncer()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (s.finished || s.items.isEmpty()) "聽寫練習" else "聽寫 ${s.index + 1} / ${s.items.size}") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val item = s.current
            when {
                s.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                s.finished -> Summary(s, onAgain = vm::restart, onBack = onBack)
                item == null -> Text("題庫裡沒有可聽寫的句子", Modifier.padding(24.dp))
                else -> {
                    // 換句時自動念一次
                    LaunchedEffect(item.id) { pronouncer.speak(item.text) }
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LinearProgressIndicator(progress = { (s.index + 1f) / s.items.size }, modifier = Modifier.fillMaxWidth())
                        Text("聽完把整句英文打出來（大小寫、標點不影響）", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pronouncer.speak(item.text) }) { Text("🔊 再聽一次") }
                            OutlinedButton(onClick = { pronouncer.speak(item.text, slow = true) }) { Text("🐢 慢速") }
                        }
                        OutlinedTextField(
                            value = s.input,
                            onValueChange = vm::onInput,
                            enabled = s.result == null,
                            placeholder = { Text("在這裡輸入你聽到的句子") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        )
                        val r = s.result
                        if (r == null) {
                            Button(onClick = vm::check, enabled = s.input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("檢查") }
                        } else {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        (if (r.passed) "✅ " else "📝 ") + "答對 ${r.correct} / ${r.words.size} 個字（${(r.ratio * 100).toInt()}%）",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (r.passed) CorrectGreen else WrongRed,
                                    )
                                    Text("正確句子（紅色 = 漏掉或拼錯）：", style = MaterialTheme.typography.bodySmall)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        r.words.forEach { w ->
                                            val ok = w.status == DictationChecker.Status.CORRECT
                                            Text(
                                                w.word,
                                                color = if (ok) CorrectGreen else WrongRed,
                                                textDecoration = if (ok) null else TextDecoration.Underline,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                }
                            }
                            Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) {
                                Text(if (s.index >= s.items.lastIndex) "看結果" else "下一句")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(s: DictationUiState, onAgain: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("聽寫完成！", style = MaterialTheme.typography.headlineMedium)
        Text("平均正確率 ${(s.averageRatio * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
        Text("完全正確 ${s.results.count { it.ratio >= 1.0 }} / ${s.results.size} 句", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text("再練 10 句") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
        s.results.forEachIndexed { i, r ->
            if (r.ratio < 1.0) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("第 ${i + 1} 句・${(r.ratio * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        Text(r.words.joinToString(" ") { it.word }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
