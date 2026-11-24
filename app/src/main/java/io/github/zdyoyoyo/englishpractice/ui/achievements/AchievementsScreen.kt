package io.github.zdyoyoyo.englishpractice.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.domain.Achievement
import io.github.zdyoyoyo.englishpractice.domain.AchievementContext
import io.github.zdyoyoyo.englishpractice.domain.Achievements
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AchievementsViewModel(private val c: AppContainer) : ViewModel() {
    val unlocked: StateFlow<Map<String, Long>> = c.achievements.unlocked
    private val _ctx = MutableStateFlow<AchievementContext?>(null)
    val ctx: StateFlow<AchievementContext?> = _ctx

    fun refresh() {
        viewModelScope.launch {
            c.achievements.checkAndUnlock()
            _ctx.value = c.achievements.context()
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { AchievementsViewModel(appContainer()) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit, vm: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)) {
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val ctx by vm.ctx.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    // 已解鎖的排前面（依解鎖時間），未解鎖的照原本順序
    val sorted = Achievements.ALL.sortedWith(compareBy({ it.id !in unlocked }, { -(unlocked[it.id] ?: 0L) }))
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("成就（${unlocked.size} / ${Achievements.ALL.size}）") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text("持續練習就能解鎖更多成就。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(sorted, key = { it.id }) { a -> AchievementCard(a, unlocked[a.id], ctx) }
        }
    }
}

@Composable
private fun AchievementCard(a: Achievement, unlockedAt: Long?, ctx: AchievementContext?) {
    val done = unlockedAt != null
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(a.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(if (done) 1f else 0.3f))
            Text(a.title, style = MaterialTheme.typography.titleSmall)
            Text(a.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            if (done) {
                Text("${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(unlockedAt!!))} 解鎖",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            } else if (ctx != null) {
                val cur = a.current(ctx)
                LinearProgressIndicator(progress = { cur.toFloat() / a.target }, modifier = Modifier.fillMaxWidth())
                Text("$cur / ${a.target}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** 新解鎖成就的提示卡（結算畫面、首頁用） */
@Composable
fun AchievementBanner(list: List<Achievement>, onDismiss: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏅 解鎖新成就！", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onDismiss != null) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) { Text("知道了") }
                }
            }
            list.forEach { Text("${it.emoji} ${it.title}：${it.description}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
