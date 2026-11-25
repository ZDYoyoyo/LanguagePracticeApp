package io.github.zdyoyoyo.englishpractice.ui.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.zdyoyoyo.englishpractice.data.NoteRepository

/** 「📝 筆記」按鈕＋顯示既有筆記；點了開啟編輯視窗 */
@Composable
fun NoteSection(note: String?, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Column {
        if (!note.isNullOrBlank()) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()) {
                Text("📝 $note", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
            }
        }
        TextButton(onClick = { editing = true }) { Text(if (note.isNullOrBlank()) "📝 寫筆記" else "✏️ 編輯筆記") }
    }
    if (editing) {
        NoteDialog(initial = note.orEmpty(), onDismiss = { editing = false }, onSave = { onSave(it); editing = false })
    }
}

@Composable
fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("我的筆記") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= NoteRepository.MAX_LENGTH) text = it },
                    placeholder = { Text("例如：by 是期限，until 是持續") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
                Text("${text.length} / ${NoteRepository.MAX_LENGTH}・清空後儲存 = 刪除筆記", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("儲存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

