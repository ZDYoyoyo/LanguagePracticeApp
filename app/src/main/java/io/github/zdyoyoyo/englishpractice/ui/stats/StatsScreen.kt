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
fun StatsScreen(onBack: () -> Unit, vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("學習統計") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
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
