package io.github.zdyoyoyo.englishpractice.ui.pk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.domain.BotProfile
import io.github.zdyoyoyo.englishpractice.domain.PkEngine
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PkScreen(onBack: () -> Unit, vm: PkViewModel = viewModel(factory = PkViewModel.Factory)) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("PK 對戰") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (s.phase) {
                PkPhase.CHOOSE_BOT -> ChooseBot(s, vm::start)
                PkPhase.PLAYING, PkPhase.ROUND_RESULT -> Playing(s, vm)
                PkPhase.FINISHED -> Finished(s, onAgain = vm::backToChooser, onBack = onBack)
            }
        }
    }
}

@Composable
private fun ChooseBot(s: PkUiState, onPick: (BotProfile) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("選一位對手（單機模擬，不需連線）", style = MaterialTheme.typography.titleMedium)
        Text("每場 ${PkEngine.QUESTIONS_PER_MATCH} 題，每題限時 ${PkEngine.TIME_LIMIT_MS / 1000} 秒。答對得 100 分，越快加分越多。")
        BotProfile.ALL.forEach { bot ->
            ElevatedCard(onClick = { onPick(bot) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(bot.emoji, style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(bot.name, style = MaterialTheme.typography.titleMedium)
                        Text("正確率約 ${(bot.accuracy * 100).toInt()}%・平均 ${bot.meanMs / 1000.0} 秒")
                    }
                }
            }
        }
        s.message?.let { Text(it, color = WrongRed) }
    }
}

@Composable
private fun Playing(s: PkUiState, vm: PkViewModel) {
    val q = s.current ?: return
    val revealed = s.phase == PkPhase.ROUND_RESULT
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Scoreboard(s)
        LinearProgressIndicator(
            progress = { (1f - s.elapsedMs.toFloat() / PkEngine.TIME_LIMIT_MS).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "第 ${s.index + 1}/${s.questions.size} 題・剩 ${((PkEngine.TIME_LIMIT_MS - s.elapsedMs).coerceAtLeast(0) / 1000)} 秒" +
                if (s.botHasAnswered && !revealed) "・${s.bot.emoji} 對手已作答！" else "",
        )
        Text(q.prompt, style = MaterialTheme.typography.titleLarge)
        q.options.forEach { option ->
            val color: Color? = when {
                !revealed -> null
                option == q.answer -> CorrectGreen
                option == s.selected -> WrongRed
                else -> null
            }
            OutlinedButton(
                onClick = { vm.answer(option) },
                enabled = !revealed,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(if (color != null) 2.dp else 1.dp, color ?: MaterialTheme.colorScheme.outline),
            ) { Text(option, modifier = Modifier.padding(vertical = 6.dp)) }
        }
        if (revealed) {
            val bot = s.botMove!!
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("你：${if (s.lastMyCorrect) "✅" else "❌"} ${s.lastMyTimeMs / 100 / 10.0} 秒")
                    Text("${s.bot.name}：${if (bot.correct) "✅" else "❌"} ${bot.timeMs / 100 / 10.0} 秒")
                    if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.index >= s.questions.lastIndex) "看結果" else "下一題")
            }
        }
    }
}

@Composable
private fun Scoreboard(s: PkUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("🙂 你 ${s.me.score}", style = MaterialTheme.typography.titleLarge)
        Text("${s.botSide.score} ${s.bot.name} ${s.bot.emoji}", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Finished(s: PkUiState, onAgain: () -> Unit, onBack: () -> Unit) {
    val outcome = PkEngine.outcome(s.me, s.botSide)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            when (outcome) {
                PkEngine.Outcome.WIN -> "🏆 你贏了！"
                PkEngine.Outcome.LOSE -> "😤 差一點，再來一場！"
                PkEngine.Outcome.DRAW -> "🤝 平手"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text("你：${s.me.score} 分・答對 ${s.me.correct} 題・總時間 ${s.me.totalTimeMs / 1000.0} 秒")
        Text("${s.bot.name}：${s.botSide.score} 分・答對 ${s.botSide.correct} 題・總時間 ${s.botSide.totalTimeMs / 1000.0} 秒")
        Button(onClick = onAgain) { Text("再來一場") }
        OutlinedButton(onClick = onBack) { Text("返回首頁") }
    }
}
