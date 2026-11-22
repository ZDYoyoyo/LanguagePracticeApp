package io.github.zdyoyoyo.englishpractice.ui.favorites

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
import androidx.compose.material.icons.filled.Star
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
import io.github.zdyoyoyo.englishpractice.ui.theme.FavoriteGold

/** 我的收藏：和錯題本不同，練習答對也不會自動移除，要自己取消收藏 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onPractice: () -> Unit,
    vm: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val pronouncer = rememberPronouncer()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("我的收藏（${items.size}）") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (items.isEmpty()) {
                Text(
                    "還沒有收藏的題目。\n\n作答時按右上角 ☆ 就能收藏；答對但其實是猜的，可以按「🎲 其實是猜的」，會自動收藏並在明天的複習中再出現。",
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                return@Column
            }
            Button(onClick = onPractice, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("練習收藏的題目")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.questionId }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    item.question?.prompt ?: "（題庫已移除此題：${item.questionId}）",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                item.question?.let { Text("正解：${it.answer}", color = CorrectGreen) }
                                item.question?.explanation?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            item.question?.speakableText?.let { text ->
                                IconButton(onClick = { pronouncer.speak(text) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "聽發音")
                                }
                            }
                            IconButton(onClick = { vm.remove(item.questionId) }) {
                                Icon(Icons.Filled.Star, contentDescription = "取消收藏", tint = FavoriteGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
