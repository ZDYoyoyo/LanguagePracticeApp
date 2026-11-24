package io.github.zdyoyoyo.englishpractice.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import io.github.zdyoyoyo.englishpractice.share.ShareCardRenderer
import io.github.zdyoyoyo.englishpractice.share.ShareCards
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.zdyoyoyo.englishpractice.domain.SkillLabels
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.domain.StatsCalculator
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onPracticeSkill: (String) -> Unit = {},
    vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("學習統計") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = {
                summary?.let { s ->
                    IconButton(onClick = {
                        ShareCardRenderer.share(context, ShareCards.forProgress(s, vm.bestMock, vm.achievementCount))
                    }) { Icon(Icons.Filled.Share, contentDescription = "分享學習成果") }
                }
            },
        )
    }) { padding ->
        val s = summary
        if (s == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("累計作答次數", "${s.totalAttempts}", Modifier.weight(1f))
                StatTile("正確率", percent(s.accuracy), Modifier.weight(1f))
                StatTile("連續打卡", "${s.streak.current} 天", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("做過的題目", "${s.answeredQuestions}", Modifier.weight(1f))
                StatTile("已熟練", "${s.matureQuestions}", Modifier.weight(1f))
                StatTile("學習天數", "${s.studyDays}", Modifier.weight(1f))
            }
            Text("「累計作答次數」= 每回答一題算一次（同一題做兩次算兩次）；「做過的題目」不重複計算。", style = MaterialTheme.typography.bodySmall)
            Text("「已熟練」= 複習間隔已拉長到 ${StatsCalculator.MATURE_INTERVAL_DAYS} 天以上的題目", style = MaterialTheme.typography.bodySmall)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("題庫完成度", style = MaterialTheme.typography.titleMedium)
                    val coverage = if (s.totalQuestions == 0) 0f else s.answeredQuestions.toFloat() / s.totalQuestions
                    LinearProgressIndicator(progress = { coverage }, modifier = Modifier.fillMaxWidth())
                    Text("${s.answeredQuestions} / ${s.totalQuestions} 題（${percent(coverage.toDouble())}）")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("近 7 天作答數", style = MaterialTheme.typography.titleMedium)
                    WeekBars(s.last7Days)
                }
            }

            WeaknessCard(s, onPracticeSkill)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("各題型表現", style = MaterialTheme.typography.titleMedium)
                    s.byCategory.forEach { c ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Text(c.category.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(
                                    if (c.attempts == 0) "還沒做過" else "正確率 ${percent(c.accuracy)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            LinearProgressIndicator(progress = { c.coverage.toFloat() }, modifier = Modifier.fillMaxWidth())
                            Text("做過 ${c.answeredQuestions} / ${c.totalQuestions} 題", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    weakest(s)?.let {
                        Text("💡 建議加強：${it.category.label}（正確率最低），可以在「開始做題」只選這個題型練習。",
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/** 錯題分析：最弱的考點 + 「練這個」；也可以展開看全部考點 */
@Composable
private fun WeaknessCard(s: StatsCalculator.Summary, onPracticeSkill: (String) -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val weakest = s.weakestSkills()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎯 弱點分析", style = MaterialTheme.typography.titleMedium)
            if (weakest.isEmpty()) {
                Text(
                    "每個考點至少作答 ${StatsCalculator.MIN_ATTEMPTS_FOR_WEAKNESS} 次才會開始分析。多做幾題再回來看看！",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text("你最常錯的考點（點「練這個」只出這類題目）：", style = MaterialTheme.typography.bodySmall)
                weakest.forEach { st -> SkillRow(st, onPracticeSkill) }
            }
            TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "收起" else "看全部考點") }
            if (showAll) {
                SkillLabels.Group.entries.forEach { g ->
                    val rows = s.bySkill.filter { st -> SkillLabels.inGroup(g).any { it.key == st.skill } }
                    if (rows.isNotEmpty()) {
                        Text(g.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        rows.sortedBy { SkillLabels.ALL.indexOfFirst { k -> k.key == it.skill } }.forEach { st -> SkillRow(st, onPracticeSkill) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillRow(st: StatsCalculator.SkillStat, onPracticeSkill: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(SkillLabels.label(st.skill), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (st.attempts == 0) "還沒做過（共 ${st.questions} 題）"
                else "正確率 ${percent(st.accuracy)}・作答 ${st.attempts} 次・共 ${st.questions} 題",
                style = MaterialTheme.typography.bodySmall,
            )
            if (st.attempts > 0) {
                LinearProgressIndicator(progress = { st.accuracy.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
            }
        }
        TextButton(onClick = { onPracticeSkill(st.skill) }) { Text("練這個") }
    }
}

/** 做過至少 3 次的題型中，正確率最低的 */
private fun weakest(s: StatsCalculator.Summary) =
    s.byCategory.filter { it.attempts >= 3 }.minByOrNull { it.accuracy }?.takeIf { it.accuracy < 0.8 }

private fun percent(v: Double) = "${(v * 100).toInt()}%"

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 簡單的長條圖：不用額外函式庫，用 Box 的高度畫 */
@Composable
private fun WeekBars(days: List<StatsCalculator.DayCount>) {
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                Text("${d.count}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f - d.count.toFloat() / max + 0.001f))
                Box(
                    Modifier
                        .width(22.dp)
                        .weight(d.count.toFloat() / max + 0.001f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    LocalDate.ofEpochDay(d.epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.TAIWAN),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
