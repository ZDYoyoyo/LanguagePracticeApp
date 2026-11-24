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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import io.github.zdyoyoyo.englishpractice.ui.achievements.AchievementBanner
import io.github.zdyoyoyo.englishpractice.share.ShareCardRenderer
import io.github.zdyoyoyo.englishpractice.share.ShareCards
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
    var confirmSubmit by remember { mutableStateOf(false) }
    if (confirmSubmit) {
        val answered = s.answers.size
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("確定交卷？") },
            text = { Text("已作答 $answered / ${s.questions.size} 題，沒作答的題目會算錯。") },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; vm.onSubmitMock() }) { Text("交卷") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("繼續作答") } },
        )
    }
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
            if (s.isMock && !s.finished && s.current != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { vm.goToQuestion(s.index - 1) }, enabled = s.index > 0, modifier = Modifier.weight(1f)) {
                            Text("上一題")
                        }
                        Text("${s.answers.size}/${s.questions.size} 已答", style = MaterialTheme.typography.bodySmall)
                        if (s.isLast) {
                            Button(onClick = { confirmSubmit = true }, modifier = Modifier.weight(1f)) { Text("交卷") }
                        } else {
                            Button(onClick = { vm.goToQuestion(s.index + 1) }, modifier = Modifier.weight(1f)) { Text("下一題") }
                        }
                    }
                }
            } else if (s.revealed && !s.finished && s.current != null) {
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
                s.finished && s.mockScore != null -> MockResultSummary(
                    s, onBack, onPlayAgain = vm::onPlayAgain,
                    onFinishedShown = { onSessionFinished(s.questions.size) },
                )
                s.finished -> ResultSummary(
                    s, vm.canPlayAgain, vm.canRetryWrong, onBack,
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
        if (s.isMock) QuestionPalette(s, onPick = vm::goToQuestion)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${s.index + 1} / ${s.questions.size}")
            AssistChip(onClick = {}, label = { Text(q.type.label) })
            AssistChip(onClick = {}, label = { Text(Difficulty.label(q.difficulty)) })
        }
        q.passage?.let { PassageCard(q, it, revealed = s.revealed, pronouncer = pronouncer, allowHelp = !s.isMock) }
        Text(q.prompt, style = MaterialTheme.typography.titleLarge)

        if (s.isMock) {
            Text("選了可以再改；交卷前都能用下方按鈕或題號回到任一題。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = vm::onGiveUp) { Text("⏭ 先跳過，稍後再答") }
        } else if (!s.revealed) {
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

/**
 * 閱讀短文 / 聽力對話。
 * - 聽力題在作答前隱藏原文（可以按「顯示原文」偷看）
 * - 原文可長按選取，用手機內建的「翻譯」查任何字
 * - 「生字」列出重點單字（可隨時打開）；「中文翻譯」作答後才能看
 */
@Composable
private fun PassageCard(q: Question, passage: String, revealed: Boolean, pronouncer: Pronouncer, allowHelp: Boolean = true) {
    var showText by remember(q.id) { mutableStateOf(false) }
    var ttsMissing by remember(q.id) { mutableStateOf(false) }
    var showGlossary by remember(q.id) { mutableStateOf(false) }
    var showTranslation by remember(q.id) { mutableStateOf(false) }
    val textVisible = !q.listening || revealed || showText
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (q.listening) {
                Text("🎧 聽力題：先聽對話再作答", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ttsMissing = !pronouncer.speakDialogue(passage) }) { Text("🔊 播放") }
                    OutlinedButton(onClick = { ttsMissing = !pronouncer.speakDialogue(passage, slow = true) }) { Text("🐢 慢速") }
                    if (!textVisible && allowHelp) TextButton(onClick = { showText = true }) { Text("顯示原文") }
                }
                if (ttsMissing) Text("這支手機沒有英文語音，請到系統設定安裝「文字轉語音」英文語音資料", color = WrongRed)
            }
            if (textVisible) {
                // SelectionContainer：長按可以選取文字 → 複製 / 用手機的「翻譯」查單字
                SelectionContainer { Text(passage, style = MaterialTheme.typography.bodyMedium) }
                Text("💡 長按文字可選取，用手機的「翻譯」查任何單字", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (q.glossary.isNotEmpty() && allowHelp) {
                    TextButton(onClick = { showGlossary = !showGlossary }) {
                        Text(if (showGlossary) "收起生字" else "📖 生字（${q.glossary.size}）")
                    }
                }
                if (revealed && !q.passageTranslation.isNullOrBlank()) {
                    TextButton(onClick = { showTranslation = !showTranslation }) {
                        Text(if (showTranslation) "收起翻譯" else "🈶 中文翻譯")
                    }
                }
            }
            if (showGlossary) {
                q.glossary.forEach { (word, meaning) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(0.45f))
                        Text(meaning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f))
                        IconButton(onClick = { pronouncer.speak(word.replace("…", "")) }, modifier = Modifier.weight(0.1f)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "聽 $word")
                        }
                    }
                }
            }
            if (showTranslation && revealed) {
                HorizontalDivider()
                SelectionContainer { Text(q.passageTranslation.orEmpty(), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun OptionButton(option: String, s: QuizUiState, q: Question, onClick: () -> Unit) {
    val color: Color? = when {
        s.isMock -> if (s.answers[q.id] == option) MaterialTheme.colorScheme.primary else null
        !s.revealed -> null
        option == q.answer -> CorrectGreen
        option == s.selected -> WrongRed
        else -> null
    }
    OutlinedButton(
        onClick = onClick,
        enabled = s.isMock || !s.revealed || color != null,
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
    canRetryWrong: Boolean,
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
        if (s.newAchievements.isNotEmpty()) AchievementBanner(s.newAchievements)

        if (canRetryWrong && s.wrongInSession.isNotEmpty()) {
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
            Text("點題目可以展開完整題目與詳解", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            s.wrongInSession.forEach { q ->
                ReviewQuestionCard(number = s.questions.indexOfFirst { it.id == q.id } + 1, q = q, mine = s.answers[q.id], correct = false)
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


/** 模擬考結算：預估分數、聽力／閱讀分項、逐題詳解 */
@Composable
private fun MockResultSummary(
    s: QuizUiState,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit,
    onFinishedShown: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(s.questions) { onFinishedShown() }
    val score = s.mockScore ?: return
    var showReview by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s.mockTimedOut) Text("⏰ 時間到，已自動交卷", color = WrongRed)
        if (s.newAchievements.isNotEmpty()) AchievementBanner(s.newAchievements)
        Text("預估多益分數", style = MaterialTheme.typography.titleMedium)
        Text("${score.estimatedTotal}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text("可能落在 ${score.low}～${score.high} 分", style = MaterialTheme.typography.bodyMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text("🎧 聽力", Modifier.weight(1f))
                    Text("${score.listeningCorrect}/${score.listeningTotal} 題・約 ${score.listeningScore} 分")
                }
                Row {
                    Text("📖 閱讀", Modifier.weight(1f))
                    Text("${score.readingCorrect}/${score.readingTotal} 題・約 ${score.readingScore} 分")
                }
                val sec = s.mockDurationMs / 1000
                Text("用時 %d 分 %02d 秒".format(sec / 60, sec % 60), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            "※ 依正確率粗估，非官方換算。題數少誤差較大，建議多考幾次看趨勢。答錯的題目已收進錯題本。",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { showReview = !showReview }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showReview) "收起詳解" else "看每一題詳解（答錯 ${s.wrongInSession.size} 題）")
        }
        val context = LocalContext.current
        OutlinedButton(
            onClick = { ShareCardRenderer.share(context, ShareCards.forMock(score, streakDays = 0)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("📤 分享成績") }
        OutlinedButton(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("再考一次") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }

        if (showReview) {
            HorizontalDivider()
            Text("點題目可以展開完整題目與詳解", style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            val wrongIds = s.wrongInSession.map { it.id }.toSet()
            s.questions.forEachIndexed { i, q ->
                ReviewQuestionCard(number = i + 1, q = q, mine = s.answers[q.id], correct = q.id !in wrongIds)
            }
        }
    }
}


/** 模擬考題號列：已答的題號是實心，目前這題有框線，點了直接跳過去 */
@Composable
private fun QuestionPalette(s: QuizUiState, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        s.questions.forEachIndexed { i, q ->
            val answered = q.id in s.answers
            val current = i == s.index
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (current) 2.dp else 0.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    .clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${i + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (answered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 詳解卡：收合時只顯示題號、對錯和題目；點開後顯示完整原題（短文 / 聽力稿）、
 * 所有選項（正解綠色、你選錯的紅色）、解析、選錯那個選項的說明、生字與中文翻譯。
 */
@Composable
fun ReviewQuestionCard(number: Int, q: Question, mine: String?, correct: Boolean) {
    var expanded by remember(q.id) { mutableStateOf(false) }
    var showTranslation by remember(q.id) { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("$number. ${if (correct) "✅" else "❌"} ", style = MaterialTheme.typography.bodyMedium)
                Text(q.prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
            }
            if (!expanded && !correct) {
                Text("你的答案：${mine ?: "（未作答）"}", color = WrongRed, style = MaterialTheme.typography.bodySmall)
                Text("正解：${q.answer}", color = CorrectGreen, style = MaterialTheme.typography.bodySmall)
            }
            if (expanded) {
                q.passage?.let { passage ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (q.listening) Text("🎧 聽力原文", style = MaterialTheme.typography.labelMedium)
                            SelectionContainer { Text(passage, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                if (q.isTypingQuestion) {
                    Text("你的答案：${mine ?: "（未作答）"}", color = if (correct) CorrectGreen else WrongRed, style = MaterialTheme.typography.bodyMedium)
                    Text("正解：${q.answer}", color = CorrectGreen, style = MaterialTheme.typography.bodyMedium)
                } else {
                    q.options.forEach { opt ->
                        val isAnswer = opt == q.answer
                        val isMine = opt == mine
                        Text(
                            (if (isAnswer) "✔ " else if (isMine) "✘ " else "・ ") + opt + (if (isMine) "（你的答案）" else ""),
                            color = when {
                                isAnswer -> CorrectGreen
                                isMine -> WrongRed
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (mine == null) Text("（這題未作答）", color = WrongRed, style = MaterialTheme.typography.bodySmall)
                }
                if (q.explanation.isNotBlank()) {
                    HorizontalDivider()
                    Text("💡 ${q.explanation}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!correct && mine != null) {
                    q.optionNotes[mine]?.let { Text("你選的「$mine」：$it", color = WrongRed, style = MaterialTheme.typography.bodySmall) }
                }
                if (q.glossary.isNotEmpty()) {
                    Text("📖 生字：" + q.glossary.entries.joinToString("、") { "${it.key} ${it.value}" }, style = MaterialTheme.typography.bodySmall)
                }
                if (!q.passageTranslation.isNullOrBlank()) {
                    TextButton(onClick = { showTranslation = !showTranslation }) {
                        Text(if (showTranslation) "收起翻譯" else "🈶 中文翻譯")
                    }
                    if (showTranslation) Text(q.passageTranslation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
