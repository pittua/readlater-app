package com.yomiato.ui.list

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yomiato.data.util.UrlUtils

/** S-3: アプリ内で URL を貼り付けて保存するダイアログ。クリップボードの URL を候補提示。 */
@Composable
fun AddUrlDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardUrl = remember { clipboardUrl(context) }
    var text by remember { mutableStateOf(clipboardUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("URL を追加") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("https://…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!clipboardUrl.isNullOrBlank() && text != clipboardUrl) {
                    TextButton(
                        onClick = { text = clipboardUrl },
                        modifier = Modifier.padding(top = 4.dp),
                    ) { Text("クリップボードの URL を使う") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text); onDismiss() },
                enabled = text.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

private fun clipboardUrl(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.coerceToText(context)?.toString()
    return UrlUtils.extractFirstUrl(text)
}
