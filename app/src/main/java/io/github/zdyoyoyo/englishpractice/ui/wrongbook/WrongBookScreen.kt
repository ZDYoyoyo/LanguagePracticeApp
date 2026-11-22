package io.github.zdyoyoyo.englishpractice.ui.wrongbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.speech.rememberPronouncer
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongBookScreen(
    onBack: () -> Unit,
    onRedo: () -> Unit,
    vm: WrongBookViewModel = viewModel(factory = WrongBookViewModel.Factory),
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val pronouncer = rememberPronouncer()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("錯題本（${items.size}）") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (items.isEmpty()) {
                Text("目前沒有錯題。答錯的題目會自動收錄在這裡。", modifier = Modifier.padding(vertical = 24.dp))
                return@Column
            }
            Button(onClick = onRedo, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("重做錯題（答對即移出錯題本）")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.record.questionId }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.question?.prompt ?: "（題庫已移除此題：${item.record.questionId}）",
                                    style = MaterialTheme.typography.titleSmall)
                                item.question?.let { Text("正解：${it.answer}", color = CorrectGreen) }
                                Text("你的答案：${item.record.lastWrongAnswer}", color = WrongRed)
                                Text("答錯 ${item.record.wrongCount} 次", style = MaterialTheme.typography.bodySmall)
                            }
                            item.question?.speakableText?.let { text ->
                                IconButton(onClick = { pronouncer.speak(text) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "聽發音")
                                }
                            }
                            IconButton(onClick = { vm.remove(item.record.questionId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "移除")
                            }
                        }
                    }
                }
            }
        }
    }
}
