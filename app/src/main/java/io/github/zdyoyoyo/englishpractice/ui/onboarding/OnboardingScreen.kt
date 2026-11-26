package io.github.zdyoyoyo.englishpractice.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.englishpractice.AppContainer
import io.github.zdyoyoyo.englishpractice.data.ProgressRepository
import io.github.zdyoyoyo.englishpractice.notify.ReminderScheduler
import io.github.zdyoyoyo.englishpractice.ui.appContainer
import kotlinx.coroutines.launch

class OnboardingViewModel(private val app: android.app.Application, private val c: AppContainer) : ViewModel() {
    fun finish(goal: Int, reminder: Boolean) {
        c.progress.setDailyGoal(goal)
        c.prefs.reminderEnabled = reminder
        if (reminder) ReminderScheduler.schedule(app, c.prefs.reminderMinuteOfDay)
        c.prefs.onboardingDone = true
    }

    companion object {
        val Factory = viewModelFactory { initializer { OnboardingViewModel(this[APPLICATION_KEY]!!, appContainer()) } }
    }
}

/**
 * 第一次打開 App 的 3 頁引導：
 * 1. 這個 App 能做什麼  2. 每日目標 + 提醒  3. 資料存在哪裡，最後可以先做模擬考測程度
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onDone: (startWithMock: Boolean) -> Unit,
    vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var goal by remember { mutableIntStateOf(10) }
    var reminder by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> reminder = granted }

    fun finish(mock: Boolean) {
        vm.finish(goal, reminder)
        onDone(mock)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                if (pager.currentPage < 2) TextButton(onClick = { finish(false) }) { Text("略過") }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (page) {
                        0 -> {
                            Text("👋", style = MaterialTheme.typography.displayLarge)
                            Text("歡迎使用英語練習", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                            Text("專為多益與職場英文設計", style = MaterialTheme.typography.titleMedium)
                            Feature("🎧", "聽力、閱讀、單字、文法", "照多益題型出題，每個選項都有解析")
                            Feature("🧠", "記得更久", "錯題本 + 間隔複習，自動安排該複習的題目")
                            Feature("📝", "模擬考", "限時作答，算出你的預估多益分數")
                            Feature("📴", "離線也能練", "不用註冊，打開就能用")
                        }
                        1 -> {
                            Text("🎯", style = MaterialTheme.typography.displayLarge)
                            Text("每天想練幾題？", style = MaterialTheme.typography.headlineMedium)
                            Text("小目標比較容易持續，之後可以在設定裡改。", textAlign = TextAlign.Center)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProgressRepository.GOAL_OPTIONS.forEach { g ->
                                    FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text("$g 題") })
                                }
                            }
                            Spacer(Modifier.size(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("每天晚上 8 點提醒我", style = MaterialTheme.typography.titleMedium)
                                    Text("提醒今日單字、該複習的題目、別讓打卡中斷", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = reminder, onCheckedChange = { on ->
                                    if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else reminder = on
                                })
                            }
                        }
                        else -> {
                            Text("🔒", style = MaterialTheme.typography.displayLarge)
                            Text("你的資料在你手上", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                            Text("學習紀錄只存在這支手機。想換手機也保留進度，可以在「設定」用 Google 登入，備份到你自己的 Google Drive（開發者看不到）。",
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.size(8.dp))
                            Text("想知道自己現在的程度嗎？", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { finish(true) }, modifier = Modifier.fillMaxWidth()) { Text("先考一次模擬考（20 題，15 分鐘）") }
                            OutlinedButton(onClick = { finish(false) }, modifier = Modifier.fillMaxWidth()) { Text("直接開始練習") }
                        }
                    }
                }
            }
            // 頁面指示點 + 下一步
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Box(Modifier.size(if (i == pager.currentPage) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                    }
                }
                if (pager.currentPage < 2) {
                    Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("下一步") }
                }
            }
        }
    }
}

@Composable
private fun Feature(emoji: String, title: String, desc: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}
