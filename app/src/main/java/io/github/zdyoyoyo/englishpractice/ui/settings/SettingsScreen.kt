package io.github.zdyoyoyo.englishpractice.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import io.github.zdyoyoyo.englishpractice.data.prefs.DisplaySettings
import io.github.zdyoyoyo.englishpractice.data.ProgressRepository
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zdyoyoyo.englishpractice.BuildConfig
import io.github.zdyoyoyo.englishpractice.ads.AdPolicy
import io.github.zdyoyoyo.englishpractice.feedback.ContactAuthor
import io.github.zdyoyoyo.englishpractice.notify.ReminderNotifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val cloud by vm.cloud.collectAsStateWithLifecycle()
    val hints by vm.hintCredits.collectAsStateWithLifecycle()
    val rewardedReady by vm.rewardedReady.collectAsStateWithLifecycle()
    val privacyRequired by vm.privacyOptionsRequired.collectAsStateWithLifecycle()
    val adStatus by vm.adStatus.collectAsStateWithLifecycle()
    val dailyGoal by vm.dailyGoal.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val weeklyReport by vm.weeklyReport.collectAsStateWithLifecycle()
    val fontSize by vm.fontSize.collectAsStateWithLifecycle()
    val freezes by vm.freezes.collectAsStateWithLifecycle()
    var testResult by remember { mutableStateOf<String?>(null) }
    val activity = LocalActivity.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var feedbackError by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Google Drive 授權同意畫面
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.onConsentResult(it.data)
    }
    LaunchedEffect(cloud.consentIntent) {
        cloud.consentIntent?.let { consentLauncher.launch(IntentSenderRequest.Builder(it).build()) }
    }

    // Android 13+ 要先取得通知權限；使用者拒絕就不開啟提醒
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminder(granted)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("設定") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountSection(cloud, vm)
            HorizontalDivider()

            SectionTitle("外觀")
            Text("主題")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DisplaySettings.ThemeMode.entries.forEach { m ->
                    FilterChip(selected = themeMode == m, onClick = { vm.setTheme(m) }, label = { Text(m.label) })
                }
            }
            Text("字體大小")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DisplaySettings.FontSize.entries.forEach { f ->
                    FilterChip(selected = fontSize == f, onClick = { vm.setFont(f) }, label = { Text(f.label) })
                }
            }
            HorizontalDivider()

            SectionTitle("每日目標與連勝保護")
            Text("每天要做幾題？首頁會顯示進度圈。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressRepository.GOAL_OPTIONS.forEach { g ->
                    FilterChip(selected = dailyGoal == g, onClick = { vm.setDailyGoal(g) }, label = { Text("$g 題") })
                }
            }
            Text("🛡️ 連勝保護：${freezes} / ${ProgressRepository.MAX_FREEZES} 次。漏打卡一天時，可以在首頁用它補回來。")
            OutlinedButton(
                onClick = { activity?.let { vm.watchAdForFreeze(it) } },
                enabled = rewardedReady && freezes < ProgressRepository.MAX_FREEZES,
            ) {
                Text(
                    when {
                        freezes >= ProgressRepository.MAX_FREEZES -> "連勝保護已存滿"
                        rewardedReady -> "看一支廣告存一次連勝保護"
                        else -> "廣告尚未就緒"
                    }
                )
            }
            HorizontalDivider()

            SectionTitle("每日提醒")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("每日一題 / 每日單字通知")
                    Text("提醒時間 %02d:%02d".format(s.reminderMinuteOfDay / 60, s.reminderMinuteOfDay % 60),
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = s.reminderEnabled, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setReminder(on)
                    }
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("每週學習報告")
                    Text("每週日晚上 8 點，通知本週作答量與進步幅度", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = weeklyReport, onCheckedChange = vm::setWeeklyReport)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showTimePicker = true }) { Text("更改提醒時間") }
                TextButton(onClick = {
                    testResult = if (ReminderNotifier.showTest(context)) "已送出測試通知，請下拉通知欄查看"
                    else "無法發送通知：請允許本 App 的通知權限"
                }) { Text("傳送測試通知") }
            }
            testResult?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            if (s.reminderEnabled && !ReminderNotifier.canNotify(context)) {
                Text("⚠️ 通知權限沒有開啟，提醒不會出現", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "提醒可能會晚幾分鐘。小米、OPPO、vivo、華為等手機會限制背景提醒，收不到的話請在「App 資訊」把省電策略設為「無限制」並允許自啟動。",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("開啟 App 資訊（省電設定）") }
            HorizontalDivider()

            SectionTitle("提示與廣告")
            Text("提示剩餘 $hints 次。做題時可用「提示」刪去錯誤選項。")
            OutlinedButton(
                onClick = { activity?.let { vm.watchRewardedAd(it) } },
                enabled = rewardedReady,
            ) { Text(if (rewardedReady) "看一支廣告換 ${AdPolicy.HINTS_PER_REWARD} 次提示（自由選擇）" else "廣告尚未就緒") }
            Text("廣告狀態：$adStatus", style = MaterialTheme.typography.bodySmall)
            if (!rewardedReady) TextButton(onClick = vm::retryAds) { Text("重新載入廣告") }
            Text("完成幾輪題目後偶爾會出現一次全螢幕廣告；作答途中不會出現。", style = MaterialTheme.typography.bodySmall)
            if (privacyRequired) {
                OutlinedButton(onClick = { activity?.let { vm.showAdPrivacyOptions(it) } }) { Text("廣告隱私選項") }
            }
            HorizontalDivider()

            SectionTitle("意見回饋")
            if (ContactAuthor.isConfigured) {
                Text("有建議、發現題目錯誤或 App 問題，歡迎來信：")
                SelectionContainer { Text(ContactAuthor.email, color = MaterialTheme.colorScheme.primary) }
                OutlinedButton(onClick = {
                    if (!ContactAuthor.sendFeedback(context)) feedbackError = true
                }) { Text("✉️ 寄信給作者") }
                if (feedbackError) Text("找不到 Email App，請直接寄信到上面的信箱", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("（作者尚未設定聯絡信箱）", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()

            SectionTitle("資料與隱私")
            Text("學習紀錄只存在這支手機（以及你登入後自己的 Google Drive）。移除 App 會一併刪除手機上的資料。",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirmClear = true }) { Text("清除這支手機上的學習資料") }
            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) { Text("隱私權政策") }
            }
            Text("版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除學習資料？") },
            text = { Text("會刪除這支手機上的作答紀錄、錯題、複習排程與打卡紀錄，無法復原。（雲端備份不受影響）") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearLocalData() }) { Text("清除") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(s.reminderMinuteOfDay / 60, s.reminderMinuteOfDay % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setReminderTime(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
