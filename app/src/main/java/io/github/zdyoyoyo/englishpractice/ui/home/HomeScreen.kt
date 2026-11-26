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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.EmojiEvents
import io.github.zdyoyoyo.englishpractice.ui.achievements.AchievementBanner
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Quiz
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
    onStats: () -> Unit,
    onWords: () -> Unit,
    onMock: () -> Unit,
    onAchievements: () -> Unit,
    onFlashcards: () -> Unit,
    onDictation: () -> Unit,
    /** 看獎勵廣告；回傳 false = 廣告還沒準備好 */
    onWatchAdForFreeze: (onReward: () -> Unit) -> Boolean,
    onPk: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val freezeMessage by vm.freezeMessage.collectAsStateWithLifecycle()
    val newAchievements by vm.newAchievements.collectAsStateWithLifecycle()
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
            if (newAchievements.isNotEmpty()) AchievementBanner(newAchievements, onDismiss = vm::dismissAchievements)
            StreakCard(
                state,
                freezeMessage = freezeMessage,
                onUseFreeze = vm::useFreeze,
                onWatchAd = { if (!onWatchAdForFreeze { vm.onFreezeAdRewarded() }) vm.onAdUnavailable() },
            )
            state.bankMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }

            // ---- 今天 ----
            SectionHeader("今天")
            HomeActionCard(
                Icons.Filled.EventRepeat, "今日複習",
                if (state.dueCount == 0) "今天沒有到期的題目 🎉" else "今天該複習 ${state.dueCount} 題",
                onReview,
            )
            HomeActionCard(
                Icons.Filled.Today, "每日練習",
                if (state.dailyWord != null) "今日單字 + 每日一題，共 2 題" else "每天一題，養成習慣",
                onDaily,
            )

            // ---- 練習（兩欄方格，減少捲動） ----
            SectionHeader("練習")
            TileGrid(
                listOf(
                    Tile(Icons.Filled.EditNote, "開始做題", "依題型、考點選題", onPractice),
                    Tile(Icons.Filled.Quiz, "多益模擬考",
                        state.progress.latestMockScore?.let { "上次 $it 分" } ?: "測出預估分數", onMock),
                    Tile(Icons.Filled.Style, "單字卡", "滑卡片背單字", onFlashcards),
                    Tile(Icons.Filled.Hearing, "聽寫練習", "聽一句打一句", onDictation),
                    Tile(Icons.AutoMirrored.Filled.MenuBook, "錯題本",
                        if (state.wrongCount == 0) "目前沒有錯題" else "${state.wrongCount} 題待訂正", onWrongBook),
                    Tile(Icons.Filled.SportsEsports, "PK 對戰", "和電腦比速度", onPk),
                ),
            )

            // ---- 我的 ----
            SectionHeader("我的")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallActionCard(Icons.Filled.Star, "收藏", onFavorites, Modifier.weight(1f))
                SmallActionCard(Icons.Filled.Translate, "單字庫", onWords, Modifier.weight(1f))
                SmallActionCard(Icons.Filled.BarChart, "統計", onStats, Modifier.weight(1f))
                SmallActionCard(Icons.Filled.EmojiEvents, "成就", onAchievements, Modifier.weight(1f))
            }

            Text(
                "題庫 v${state.bankVersion}・共 ${state.questionCount} 題・已做過 ${state.distinctAnswered} 題・累計作答 ${state.totalAttempts} 次",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StreakCard(
    state: HomeUiState,
    freezeMessage: String?,
    onUseFreeze: () -> Unit,
    onWatchAd: () -> Unit,
) {
    val p = state.progress
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 每日目標進度圈，中間是火焰
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (p.answeredToday.toFloat() / p.goal).coerceIn(0f, 1f) },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(if (p.goalReached) "🏆" else "🔥", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text("連續打卡 ${state.streak.current} 天", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (p.goalReached) "今日目標達成 ✅ ${p.answeredToday}/${p.goal} 題"
                        else "今日目標 ${p.answeredToday}/${p.goal} 題",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        when {
                            state.streak.studiedToday -> "今天已打卡（最長紀錄 ${state.streak.longest} 天）"
                            state.streak.current > 0 -> "今天還沒打卡，做一題就能延續！"
                            else -> "今天做一題就開始打卡"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (p.repairableDay != null) {
                HorizontalDivider()
                Text("😢 昨天漏打卡了，連勝中斷。可以用「連勝保護」補回昨天。", style = MaterialTheme.typography.bodyMedium)
                if (p.freezes > 0) {
                    Button(onClick = onUseFreeze) { Text("🛡️ 使用連勝保護（剩 ${p.freezes} 次）") }
                } else {
                    OutlinedButton(onClick = onWatchAd) { Text("🎬 看一支廣告補回昨天") }
                }
            }
            freezeMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp))
}

private data class Tile(val icon: ImageVector, val title: String, val subtitle: String, val onClick: () -> Unit)

/** 兩欄方格：每格固定高度，標題 + 一行小字 */
@Composable
private fun TileGrid(tiles: List<Tile>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    ElevatedCard(onClick = t.onClick, modifier = Modifier.weight(1f).height(96.dp)) {
                        Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(t.icon, contentDescription = null)
                            Column {
                                Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(t.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallActionCard(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
