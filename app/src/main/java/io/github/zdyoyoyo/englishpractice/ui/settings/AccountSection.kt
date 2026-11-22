package io.github.zdyoyoyo.englishpractice.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.zdyoyoyo.englishpractice.sync.CloudStatus
import java.text.DateFormat
import java.util.Date

/** 帳號與雲端同步區塊。未登入時清楚說明「不登入就無法跨裝置保留資料」。 */
@Composable
fun AccountSection(status: CloudStatus, vm: SettingsViewModel) {
    val activity = LocalActivity.current
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteCloud by remember { mutableStateOf(false) }

    SectionTitle("帳號與雲端備份")
    if (status.email == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("目前未登入：可以正常使用全部功能", style = MaterialTheme.typography.titleSmall)
                Text("⚠️ 但學習紀錄只存在這支手機。換手機、移除 App 或清除資料後，進度、錯題與打卡紀錄都會消失，無法找回。")
                Text("登入 Google 後，會把進度備份到「你自己的」Google Drive App 專用資料夾（佔用你的 Drive 空間、不經過開發者的伺服器，也看不到你 Drive 裡的其他檔案）。")
            }
        }
        Button(
            onClick = { activity?.let { vm.signIn(it) } },
            enabled = status.configured && !status.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("用 Google 登入並備份") }
        if (!status.configured) {
            Text("（開發中：尚未設定 GOOGLE_WEB_CLIENT_ID，請見 README）", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Text("已登入：${status.email}")
        Text(
            if (status.lastSyncedAt > 0) "上次同步：${DateFormat.getDateTimeInstance().format(Date(status.lastSyncedAt))}"
            else "尚未同步",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("開啟 App 時會自動同步；兩邊不同時以「較新的一份」為準。", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::syncNow, enabled = !status.busy) { Text("立即同步") }
            OutlinedButton(onClick = vm::restoreFromCloud, enabled = !status.busy) { Text("從雲端還原") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { confirmDeleteCloud = true }, enabled = !status.busy) { Text("刪除雲端備份") }
            OutlinedButton(onClick = { confirmSignOut = true }, enabled = !status.busy) { Text("登出") }
        }
    }
    if (status.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    status.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

    // 第一次在這支手機同步、兩邊都有資料：讓使用者選
    status.choice?.let { choice ->
        val fmt = DateFormat.getDateTimeInstance()
        AlertDialog(
            onDismissRequest = vm::dismissChoice,
            title = { Text("雲端已有備份") },
            text = {
                Text("雲端備份：${fmt.format(Date(choice.remoteChangedAt))}\n這支手機：${fmt.format(Date(choice.localChangedAt))}\n\n要保留哪一份？另一份會被覆蓋。")
            },
            confirmButton = { TextButton(onClick = { vm.dismissChoice(); vm.restoreFromCloud() }) { Text("用雲端還原") } },
            dismissButton = { TextButton(onClick = { vm.dismissChoice(); vm.uploadLocal() }) { Text("用本機覆蓋雲端") } },
        )
    }

    if (confirmSignOut) {
        var alsoClear by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("登出 Google") },
            text = {
                Column {
                    Text("登出後不會再同步。雲端備份會保留，之後登入同一帳號可還原。")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = alsoClear, onCheckedChange = { alsoClear = it })
                        Text("同時清除這支手機上的學習資料")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut(alsoClear) }) { Text("登出") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("取消") } },
        )
    }

    if (confirmDeleteCloud) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCloud = false },
            title = { Text("刪除雲端備份？") },
            text = { Text("會刪除你 Google Drive App 資料夾中的備份檔，無法復原。手機上的資料不受影響。") },
            confirmButton = { TextButton(onClick = { confirmDeleteCloud = false; vm.deleteCloudBackup() }) { Text("刪除") } },
            dismissButton = { TextButton(onClick = { confirmDeleteCloud = false }) { Text("取消") } },
        )
    }
}
