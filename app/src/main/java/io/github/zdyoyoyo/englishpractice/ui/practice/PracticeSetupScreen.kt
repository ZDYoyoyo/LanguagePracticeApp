package io.github.zdyoyoyo.englishpractice.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.data.model.Difficulty

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(
    onBack: () -> Unit,
    onStart: (tag: String?, difficulty: Int?) -> Unit,
    vm: PracticeSetupViewModel = viewModel(factory = PracticeSetupViewModel.Factory),
) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var difficulty by rememberSaveable { mutableStateOf<Int?>(null) }
    val count = vm.countMatching(tag, difficulty)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("選擇題目") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("主題", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("全部") })
                tags.forEach { t ->
                    FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(TagLabels.label(t)) })
                }
            }
            Text("難度", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = difficulty == null, onClick = { difficulty = null }, label = { Text("全部") })
                (1..3).forEach { d ->
                    FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(Difficulty.label(d)) })
                }
            }
            Text("符合條件：$count 題（每輪最多 10 題）")
            Button(onClick = { onStart(tag, difficulty) }, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
                Text("開始")
            }
        }
    }
}

/** 主題標籤的中文顯示名稱；題庫出現新標籤時直接顯示英文 */
object TagLabels {
    private val map = mapOf(
        "daily-life" to "日常生活", "travel" to "旅遊", "work" to "職場",
        "food" to "飲食", "school" to "校園", "tech" to "科技",
    )
    fun label(tag: String) = map[tag] ?: tag
}
