package io.github.zdyoyoyo.englishpractice.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.data.model.Difficulty
import io.github.zdyoyoyo.englishpractice.domain.QuestionCategory
import io.github.zdyoyoyo.englishpractice.domain.QuestionSelector

/** 選題設定 */
data class PracticeOptions(
    val tag: String?,
    val difficulty: Int?,
    val category: QuestionCategory?,
    val count: Int,
    val preferNew: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(
    onBack: () -> Unit,
    onStart: (PracticeOptions) -> Unit,
    vm: PracticeSetupViewModel = viewModel(factory = PracticeSetupViewModel.Factory),
) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var difficulty by rememberSaveable { mutableStateOf<Int?>(null) }
    var categoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    var count by rememberSaveable { mutableIntStateOf(QuestionSelector.DEFAULT_SESSION_SIZE) }
    var preferNew by rememberSaveable { mutableStateOf(true) }
    val category = QuestionCategory.fromKey(categoryKey)
    val matching = vm.countMatching(QuestionSelector.Filter(tag, difficulty, category))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("選擇題目") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("符合條件：$matching 題・本輪最多 ${minOf(count, matching)} 題", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { onStart(PracticeOptions(tag, difficulty, category, count, preferNew)) },
                        enabled = matching > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("開始") }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("題型", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { categoryKey = null }, label = { Text("全部") })
                QuestionCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { categoryKey = c.key }, label = { Text(c.label) })
                }
            }
            Text("主題", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("全部") })
                TagLabels.sort(tags).forEach { t ->
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
            Text("每輪題數", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionSelector.SESSION_SIZES.forEach { n ->
                    FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n 題") })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("優先出沒做過、常錯的題目", style = MaterialTheme.typography.titleMedium)
                    Text("關閉則完全隨機", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = preferNew, onCheckedChange = { preferNew = it })
            }
            Text("同一篇閱讀或聽力的題目會排在一起出現。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 主題標籤的中文顯示名稱；題庫出現新標籤時直接顯示英文 */
object TagLabels {
    private val map = mapOf(
        "toeic" to "多益", "work" to "職場", "office" to "辦公室", "business" to "商業",
        "finance" to "財務", "hr" to "人資", "marketing" to "行銷",
        "daily-life" to "日常生活", "travel" to "旅遊", "food" to "飲食", "school" to "校園", "tech" to "科技",
    )
    private val order = map.keys.toList()
    fun label(tag: String) = map[tag] ?: tag
    /** 依上面的順序排列（多益排第一），未知標籤排最後 */
    fun sort(tags: List<String>) = tags.sortedBy { order.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
}


