package io.github.zdyoyoyo.englishpractice.ui.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.data.local.MockResultEntity
import io.github.zdyoyoyo.englishpractice.domain.MockTestBuilder
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date

class MockSetupViewModel(results: kotlinx.coroutines.flow.Flow<List<MockResultEntity>>) : ViewModel() {
    val history: StateFlow<List<MockResultEntity>> =
        results.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { MockSetupViewModel(appContainer().progress.observeMockResults(20)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockSetupScreen(
    onBack: () -> Unit,
    onStart: (count: Int) -> Unit,
    vm: MockSetupViewModel = viewModel(factory = MockSetupViewModel.Factory),
) {
    val history by vm.history.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("多益模擬考") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("怎麼考", style = MaterialTheme.typography.titleMedium)
                    Text("・照多益比例出題：前半聽力、後半閱讀（Part 5 單句 + Part 6/7 短文）")
                    Text("・限時作答，作答中不顯示對錯、不能用提示")
                    Text("・交卷後算出預估分數，並可以看每一題的詳解")
                    Text("・答錯的題目會自動收進錯題本")
                }
            }
            MockTestBuilder.SIZES.forEach { n ->
                Button(onClick = { onStart(n) }, modifier = Modifier.fillMaxWidth()) {
                    Text("$n 題（限時 ${MockTestBuilder.timeLimitMs(n) / 60_000} 分鐘）")
                }
            }

            if (history.isNotEmpty()) {
                Text("成績紀錄", style = MaterialTheme.typography.titleMedium)
                ScoreTrend(history.reversed().takeLast(10))
                val fmt = DateFormat.getDateInstance(DateFormat.SHORT)
                history.forEach { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${r.estimatedScore} 分", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${fmt.format(Date(r.takenAt))}・${r.totalQuestions} 題・聽力 ${r.listeningCorrect}/${r.listeningTotal}・閱讀 ${r.readingCorrect}/${r.readingTotal}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            } else {
                Text("還沒有成績，先考一次看看自己的程度吧！", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** 最近幾次的分數長條圖（0～990） */
@Composable
private fun ScoreTrend(results: List<MockResultEntity>) {
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        results.forEach { r ->
            val ratio = (r.estimatedScore / 990f).coerceIn(0.02f, 1f)
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${r.estimatedScore}", style = MaterialTheme.typography.labelSmall)
                Box(Modifier.weight(1f - ratio + 0.001f))
                Box(
                    Modifier.width(18.dp).weight(ratio)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
