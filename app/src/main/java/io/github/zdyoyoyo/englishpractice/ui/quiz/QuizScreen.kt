package io.github.zdyoyoyo.englishpractice.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.ads.AdPolicy
import io.github.zdyoyoyo.englishpractice.data.model.Difficulty
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.feedback.ContactAuthor
import io.github.zdyoyoyo.englishpractice.speech.Pronouncer
import io.github.zdyoyoyo.englishpractice.speech.PronunciationPanel
import io.github.zdyoyoyo.englishpractice.speech.rememberPronouncer
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.FavoriteGold
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    /** 一輪結束時呼叫（參數是本輪題數），用來決定要不要顯示插頁廣告 */
    onSessionFinished: (Int) -> Unit = {},
    /** 使用者同意看獎勵廣告時呼叫；回傳 false 代表廣告還沒準備好 */
    onWatchRewardedAd: (onReward: () -> Unit) -> Boolean = { false },
    vm: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val s by vm.ui.collectAsStateWithLifecycle()
    if (s.showRewardOffer) {
        AlertDialog(
            onDismissRequest = vm::dismissRewardOffer,
            title = { Text("提示次數用完了") },
            text = { Text("要看一支短廣告，換 ${AdPolicy.HINTS_PER_REWARD} 次提示嗎？\n（完全自由選擇，不看也能繼續作答）") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissRewardOffer()
                    if (!onWatchRewardedAd { vm.onRewardEarned() }) vm.onRewardUnavailable()
                }) { Text("看廣告") }
            },
            dismissButton = { TextButton(onClick = vm::dismissRewardOffer) { Text("不用了") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    if (s.current != null && !s.finished) {
                        IconButton(onClick = vm::onToggleFavorite) {
                            Icon(
                                if (s.isCurrentFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (s.isCurrentFavorite) "取消收藏" else "收藏",
                                tint = if (s.isCurrentFavorite) FavoriteGold else LocalContentColor.current,
                            )
                        }
                    }
                },
            )
        },
        // 「下一題」固定在畫面底部：長篇閱讀或解析很長時也不用一直往下滑
        bottomBar = {
            if (s.revealed && !s.finished && s.current != null) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = vm::onNext,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(if (s.isLast) "看結果" else "下一題") }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                s.emptyMessage != null -> EmptyState(s.emptyMessage!!, onBack)
                s.finished -> ResultSummary(
                    s, vm.canPlayAgain, onBack,
                    onRetryWrong = vm::onRetryWrong,
                    onPlayAgain = vm::onPlayAgain,
                    onFinishedShown = { onSessionFinished(s.questions.size) },
                )
                else -> QuestionContent(s, vm)
            }
        }
    }
}

@Composable
private fun QuestionContent(s: QuizUiState, vm: QuizViewModel) {
    val q = s.current ?: return
    val pronouncer = rememberPronouncer()
    val context = LocalContext.current
    // 換題時停止上一題還在播放的語音
    androidx.compose.runtime.LaunchedEffect(q.id) { pronouncer.stop() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinearProgressIndicator(
            progress = { (s.index + 1f) / s.questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${s.index + 1} / ${s.questions.size}")
            AssistChip(onClick = {}, label = { Text(q.type.label) })
            AssistChip(onClick = {}, label = { Text(Difficulty.label(q.difficulty)) })
        }
        q.passage?.let { PassageCard(q, it, revealed = s.revealed, pronouncer = pronouncer) }
        Text(q.prompt, style = MaterialTheme.typography.titleLarge)

        if (!s.revealed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = vm::onHint, enabled = !s.usedHint) {
                    Text(if (s.hintCredits > 0) "💡 提示（剩 ${s.hintCredits} 次）" else "💡 提示")
                }
                TextButton(onClick = vm::onGiveUp) { Text("🏳️ 不會，看答案") }
            }
        }
        s.hintText?.let { Text("💡 $it", color = MaterialTheme.colorScheme.tertiary) }
        s.rewardMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }

        if (q.isTypingQuestion) {
            TypingAnswer(s, q, vm)
        } else {
            q.options.filter { it !in s.hiddenOptions || s.revealed }
                .forEach { option -> OptionButton(option, s, q, onClick = { vm.onOptionSelected(option) }) }
        }

        if (s.revealed) {
            Feedback(s, q)
            if (s.lastCorrect) {
                if (s.markedGuessed) {
                    Text("⭐ 已加入收藏，明天會再出現在複習裡", color = MaterialTheme.colorScheme.secondary)
                } else {
                    TextButton(onClick = vm::onMarkGuessed) { Text("🎲 其實是猜的（加入收藏，明天再複習）") }
                }
            }
            q.speakableText?.let { PronunciationPanel(it, pronouncer) }
            if (ContactAuthor.isConfigured) {
                TextButton(onClick = { ContactAuthor.reportQuestion(context, q.id, q.prompt) }) {
                    Text("這題有錯？回報給作者")
                }
            }
        }
    }
}

/** 閱讀短文 / 聽力對話。聽力題在作答前隱藏原文（可以按「顯示原文」偷看） */
@Composable
private fun PassageCard(q: Question, passage: String, revealed: Boolean, pronouncer: Pronouncer) {
    var showText by remember(q.id) { mutableStateOf(false) }
    var ttsMissing by remember(q.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (q.listening) {
                Text("🎧 聽力題：先聽對話再作答", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ttsMissing = !pronouncer.speakDialogue(passage) }) { Text("🔊 播放") }
                    OutlinedButton(onClick = { ttsMissing = !pronouncer.speakDialogue(passage, slow = true) }) { Text("🐢 慢速") }
                    if (!revealed && !showText) TextButton(onClick = { showText = true }) { Text("顯示原文") }
                }
                if (ttsMissing) Text("這支手機沒有英文語音，請到系統設定安裝「文字轉語音」英文語音資料", color = WrongRed)
                if (revealed || showText) Text(passage, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(passage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun OptionButton(option: String, s: QuizUiState, q: Question, onClick: () -> Unit) {
    val color: Color? = when {
        !s.revealed -> null
        option == q.answer -> CorrectGreen
        option == s.selected -> WrongRed
        else -> null
    }
    OutlinedButton(
        onClick = onClick,
        enabled = !s.revealed || color != null,
        modifier = Modifier.fillMaxWidth(),
        border = color?.let { BorderStroke(2.dp, it) } ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(option, color = color ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun TypingAnswer(s: QuizUiState, q: Question, vm: QuizViewModel) {
    OutlinedTextField(
        value = s.typed,
        onValueChange = vm::onTypedChange,
        label = { Text("輸入答案") },
        singleLine = true,
        enabled = !s.revealed,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { vm.onSubmitTyped() }),
        modifier = Modifier.fillMaxWidth(),
    )
    if (!s.revealed) {
        Button(onClick = vm::onSubmitTyped, enabled = s.typed.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("送出")
        }
    } else if (!s.lastCorrect) {
        Text("正解：${q.answer}", color = CorrectGreen, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Feedback(s: QuizUiState, q: Question) {
    val correct = s.lastCorrect
    Card(
        colors = CardDefaults.cardColors(
            containerColor = (if (correct) CorrectGreen else WrongRed).copy(alpha = 0.12f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when {
                    correct -> "✅ 答對了！"
                    s.gaveUp -> "🏳️ 正解是「${q.answer}」，已加入錯題本"
                    else -> "❌ 答錯了，已加入錯題本"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (correct) CorrectGreen else WrongRed,
            )
            if (q.explanation.isNotBlank()) Text(q.explanation)
            s.wrongOptionNote?.let { note ->
                HorizontalDivider()
                Text("你選的「${s.selected}」", style = MaterialTheme.typography.titleSmall, color = WrongRed)
                Text(note)
            }
        }
    }
}

@Composable
private fun ResultSummary(
    s: QuizUiState,
    canPlayAgain: Boolean,
    onBack: () -> Unit,
    onRetryWrong: () -> Unit,
    onPlayAgain: () -> Unit,
    onFinishedShown: () -> Unit,
) {
    // key = 題目清單：重做 / 再來一輪後的新結算也會通知（廣告頻率由 AdPolicy 控制）
    androidx.compose.runtime.LaunchedEffect(s.questions) { onFinishedShown() }
    val total = s.questions.size
    val percent = if (total == 0) 0 else s.correctCount * 100 / total
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            when {
                percent == 100 -> "🏆 全對！"
                percent >= 70 -> "👍 表現不錯！"
                else -> "💪 再接再厲！"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text("答對 ${s.correctCount} / $total（$percent%）", style = MaterialTheme.typography.titleLarge)

        if (s.wrongInSession.isNotEmpty()) {
            Button(onClick = onRetryWrong, modifier = Modifier.fillMaxWidth()) {
                Text("重做這輪答錯的 ${s.wrongInSession.size} 題")
            }
        }
        if (canPlayAgain) {
            OutlinedButton(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("再來一輪") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }

        if (s.wrongInSession.isNotEmpty()) {
            HorizontalDivider()
            Text("這輪答錯的題目（已收進錯題本）", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth())
            s.wrongInSession.forEach { q ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(q.prompt, style = MaterialTheme.typography.bodyMedium)
                        Text("正解：${q.answer}", color = CorrectGreen, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onBack) { Text("返回") }
    }
}
