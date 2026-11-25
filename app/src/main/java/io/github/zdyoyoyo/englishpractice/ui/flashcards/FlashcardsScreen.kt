package io.github.zdyoyoyo.englishpractice.ui.flashcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.domain.Flashcards
import io.github.zdyoyoyo.englishpractice.domain.WordCard
import io.github.zdyoyoyo.englishpractice.speech.Pronouncer
import io.github.zdyoyoyo.englishpractice.speech.rememberPronouncer
import io.github.zdyoyoyo.englishpractice.ui.achievements.AchievementBanner
import io.github.zdyoyoyo.englishpractice.ui.theme.CorrectGreen
import io.github.zdyoyoyo.englishpractice.ui.theme.WrongRed
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(onBack: () -> Unit, vm: FlashcardsViewModel = viewModel(factory = FlashcardsViewModel.Factory)) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val pronouncer = rememberPronouncer()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (s.phase == CardPhase.PLAYING) "單字卡 ${s.index + 1} / ${s.cards.size}" else "單字卡") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (s.phase) {
                CardPhase.CHOOSE -> DeckChooser(s, vm::start)
                CardPhase.PLAYING -> s.current?.let { Playing(s, it, vm, pronouncer) }
                CardPhase.DONE -> Done(s, vm, onBack)
            }
        }
    }
}

@Composable
private fun DeckChooser(s: FlashcardsUiState, onPick: (Flashcards.Deck) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("翻卡片背單字：看英文想中文，點卡片翻面。右滑或按「認識」、左滑或按「不認識」。", style = MaterialTheme.typography.bodyMedium)
        Text("「不認識」的單字會排進明天的複習，但不會收進錯題本。", style = MaterialTheme.typography.bodySmall)
        Flashcards.Deck.entries.forEach { d ->
            ElevatedCard(onClick = { onPick(d) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(d.label, style = MaterialTheme.typography.titleMedium)
                    Text(d.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        s.message?.let { Text(it, color = WrongRed) }
    }
}

@Composable
private fun Playing(s: FlashcardsUiState, card: WordCard, vm: FlashcardsViewModel, pronouncer: Pronouncer) {
    val offsetX = remember(card.questionId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 換卡時自動念一次
    LaunchedEffect(card.questionId) { pronouncer.speak(card.word) }

    fun decide(known: Boolean) {
        scope.launch {
            offsetX.animateTo(if (known) 1200f else -1200f, tween(200))
            vm.answer(known)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgressIndicator(progress = { (s.index + 1f) / s.cards.size }, modifier = Modifier.fillMaxWidth())
        Row {
            Text("✅ 認識 ${s.known.size}", color = CorrectGreen, modifier = Modifier.weight(1f))
            Text("❓ 不認識 ${s.unknown.size}", color = WrongRed)
        }
        Card(
            onClick = vm::flip,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / 40f
                }
                .pointerInput(card.questionId) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val x = offsetX.value
                            if (abs(x) > 250f) decide(known = x > 0)
                            else scope.launch { offsetX.animateTo(0f, tween(150)) }
                        },
                    ) { _, dragAmount -> scope.launch { offsetX.snapTo(offsetX.value + dragAmount) } }
                },
            colors = CardDefaults.cardColors(
                containerColor = when {
                    offsetX.value > 120f -> CorrectGreen.copy(alpha = 0.15f)
                    offsetX.value < -120f -> WrongRed.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(card.word, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                IconButton(onClick = { pronouncer.speak(card.word) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "聽發音")
                }
                if (s.flipped) {
                    card.partOfSpeech?.let { Text("（$it）", style = MaterialTheme.typography.titleMedium) }
                    Text(card.meaning, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    card.example?.let { ex ->
                        Text(ex, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = { pronouncer.speak(ex) }) { Text("🔊 念例句") }
                    }
                } else {
                    Text("點卡片看中文", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { decide(known = false) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrongRed),
            ) { Text("← 不認識") }
            Button(
                onClick = { decide(known = true) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen),
            ) { Text("認識 →") }
        }
    }
}

@Composable
private fun Done(s: FlashcardsUiState, vm: FlashcardsViewModel, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("這輪完成！", style = MaterialTheme.typography.headlineMedium)
        Text("認識 ${s.known.size}・不認識 ${s.unknown.size}", style = MaterialTheme.typography.titleLarge)
        if (s.newAchievements.isNotEmpty()) AchievementBanner(s.newAchievements)
        if (s.unknown.isNotEmpty()) {
            Button(onClick = vm::retryUnknown, modifier = Modifier.fillMaxWidth()) { Text("再練不認識的 ${s.unknown.size} 個") }
        }
        OutlinedButton(onClick = vm::backToChooser, modifier = Modifier.fillMaxWidth()) { Text("換一副牌") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
        if (s.unknown.isNotEmpty()) {
            Text("不認識的單字", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            s.unknown.forEach { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.word, style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(c.partOfSpeech?.let { "（$it）" }, c.meaning).joinToString(""), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
