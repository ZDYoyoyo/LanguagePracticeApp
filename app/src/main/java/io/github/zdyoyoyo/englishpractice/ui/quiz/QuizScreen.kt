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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.data.model.Difficulty
import io.github.zdyoyoyo.englishpractice.data.model.Question
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    onFinished: (QuizViewModel) -> Unit = {},
    vm: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(s.title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                s.emptyMessage != null -> EmptyState(s.emptyMessage!!, onBack)
                s.finished -> ResultSummary(s, onBack, onFinishedShown = { onFinished(vm) })
                else -> QuestionContent(s, vm)
            }
        }
    }
}

@Composable
private fun QuestionContent(s: QuizUiState, vm: QuizViewModel) {
    val q = s.current ?: return
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
        Text(q.prompt, style = MaterialTheme.typography.titleLarge)

        if (q.isTypingQuestion) {
            TypingAnswer(s, q, vm)
        } else {
            q.options.forEach { option -> OptionButton(option, s, q, onClick = { vm.onOptionSelected(option) }) }
        }

        if (s.revealed) {
            Feedback(s.lastCorrect, q)
            Button(onClick = vm::onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.isLast) "看結果" else "下一題")
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
private fun Feedback(correct: Boolean, q: Question) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = (if (correct) CorrectGreen else WrongRed).copy(alpha = 0.12f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (correct) "✅ 答對了！" else "❌ 答錯了，已加入錯題本",
                style = MaterialTheme.typography.titleMedium,
                color = if (correct) CorrectGreen else WrongRed,
            )
            if (q.explanation.isNotBlank()) Text(q.explanation)
        }
    }
}

@Composable
private fun ResultSummary(s: QuizUiState, onBack: () -> Unit, onFinishedShown: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onFinishedShown() }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("本輪完成！", style = MaterialTheme.typography.headlineMedium)
        Text("答對 ${s.correctCount} / ${s.questions.size}", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack) { Text("返回") }
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
