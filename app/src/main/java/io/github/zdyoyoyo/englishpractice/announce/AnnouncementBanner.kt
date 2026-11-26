package io.github.zdyoyoyo.englishpractice.announce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/** 首頁最上方的公告卡片 */
@Composable
fun AnnouncementBanner(
    a: Announcement,
    onShown: () -> Unit,
    onAction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(a.id) { onShown() }
    val uri = LocalUriHandler.current
    val action = a.action?.takeIf { Announcements.isValidAction(it) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📢 ${a.title}", style = MaterialTheme.typography.titleMedium)
            if (a.body.isNotBlank()) Text(a.body, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (action != null) {
                    Button(onClick = {
                        if (action.startsWith("https://")) runCatching { uri.openUri(action) } else onAction(action)
                    }) { Text(a.actionLabel ?: "前往") }
                }
                TextButton(onClick = onDismiss) { Text("知道了") }
            }
        }
    }
}
