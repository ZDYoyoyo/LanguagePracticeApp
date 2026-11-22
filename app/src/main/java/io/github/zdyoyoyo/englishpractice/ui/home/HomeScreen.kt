package io.github.zdyoyoyo.englishpractice.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPractice: () -> Unit,
    onReview: () -> Unit,
    onDaily: () -> Unit,
    onWrongBook: () -> Unit,
    onFavorites: () -> Unit,
    onPk: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("英語練習") },
            actions = { IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "設定") } },
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StreakCard(state)
            state.bankMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }

            HomeActionCard(
                Icons.Filled.EventRepeat, "今日複習",
                if (state.dueCount == 0) "今天沒有到期的題目" else "今天該複習 ${state.dueCount} 題",
                onReview,
            )
            HomeActionCard(
                Icons.Filled.Today, "每日一題",
                state.dailyWord?.word?.let { "今日單字：$it" } ?: "每天一題，養成習慣",
                onDaily,
            )
            HomeActionCard(Icons.Filled.EditNote, "開始做題", "依主題或難度選題", onPractice)
            HomeActionCard(
                Icons.AutoMirrored.Filled.MenuBook, "錯題本",
                if (state.wrongCount == 0) "目前沒有錯題" else "還有 ${state.wrongCount} 題待訂正",
                onWrongBook,
            )
            HomeActionCard(
                Icons.Filled.Star, "我的收藏",
                if (state.favoriteCount == 0) "作答時按 ☆ 收藏想再看的題目" else "已收藏 ${state.favoriteCount} 題",
                onFavorites,
            )
            HomeActionCard(Icons.Filled.SportsEsports, "PK 對戰", "和電腦對手比速度與正確率", onPk)

            Text(
                "題庫 v${state.bankVersion}・共 ${state.questionCount} 題・累計作答 ${state.totalAttempts} 次",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StreakCard(state: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.size(12.dp))
            Column {
                Text("連續打卡 ${state.streak.current} 天", style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        state.streak.studiedToday -> "今天已打卡 ✅（最長紀錄 ${state.streak.longest} 天）"
                        state.streak.current > 0 -> "今天還沒打卡，做一題就能延續！"
                        else -> "今天做一題就開始打卡"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun HomeActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
