package io.github.zdyoyoyo.englishpractice.ui.words

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.speech.rememberPronouncer
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.FavoriteGold
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(onBack: () -> Unit, vm: WordListViewModel = viewModel(factory = WordListViewModel.Factory)) {
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val pronouncer = rememberPronouncer()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("單字庫（${items.size}）") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("搜尋英文或中文") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.question.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.word, style = MaterialTheme.typography.titleMedium)
                                Text(item.question.explanation, style = MaterialTheme.typography.bodySmall)
                                item.stat?.let { st ->
                                    Text(
                                        "答對 ${st.correct} / ${st.attempts} 次",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (st.correct == st.attempts) CorrectGreen else WrongRed,
                                    )
                                } ?: Text("還沒練習過", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { pronouncer.speak(item.word) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "聽發音")
                            }
                            IconButton(onClick = { vm.toggleFavorite(item.question.id) }) {
                                Icon(
                                    if (item.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (item.favorite) "取消收藏" else "收藏",
                                    tint = if (item.favorite) FavoriteGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
