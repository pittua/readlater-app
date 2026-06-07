package com.yomiato.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.data.settings.ThemeMode

/** S-7: 設定。表示（テーマ/フォント/行間）・動作（自動既読）・データ（全削除）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("設定") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            SectionHeader("表示")

            SettingLabel("テーマ")
            ThemeMode.entries.forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setThemeMode(mode) }
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                ) {
                    RadioButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                    Text(
                        text = when (mode) {
                            ThemeMode.SYSTEM -> "システムに従う"
                            ThemeMode.LIGHT -> "ライト"
                            ThemeMode.DARK -> "ダーク"
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            SwitchRow(
                title = "ダイナミックカラー",
                subtitle = "壁紙から配色（Android 12+）",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )

            SliderRow(
                title = "本文フォントサイズ",
                value = settings.readerFontScale,
                valueRange = 0.8f..1.6f,
                steps = 7,
                valueLabel = "${(settings.readerFontScale * 100).toInt()}%",
                onValueChange = viewModel::setFontScale,
            )

            SliderRow(
                title = "本文の行間",
                value = settings.readerLineHeightScale,
                valueRange = 0.8f..1.5f,
                steps = 6,
                valueLabel = "${(settings.readerLineHeightScale * 100).toInt()}%",
                onValueChange = viewModel::setLineHeightScale,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("動作")

            SwitchRow(
                title = "開いたら自動で既読にする",
                subtitle = null,
                checked = settings.autoMarkRead,
                onCheckedChange = viewModel::setAutoMarkRead,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("AI（要約・タグ）")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiKeyDialog = true }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Anthropic API キー", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (hasApiKey) "設定済み（タップで変更）。記事をAIで要約・タグ付けできます。"
                    else "未設定。キーを入れると記事の要約・タグ提案が使えます（本文が外部APIに送信されます）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasApiKey) {
                Text(
                    text = "API キーを削除",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearApiKey() }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("データ")

            Text(
                text = "すべての記事を削除",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearConfirm = true }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Yomiato 0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("すべての記事を削除") },
            text = { Text("保存したすべての記事を削除します。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("キャンセル") }
            },
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onSave = { viewModel.setApiKey(it); showApiKeyDialog = false },
            onDismiss = { showApiKeyDialog = false },
        )
    }
}

@Composable
private fun ApiKeyDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anthropic API キー") },
        text = {
            Column {
                Text(
                    "console.anthropic.com で発行したキー（sk-ant-…）を貼り付けてください。" +
                        "キーは端末内に暗号化保存され、AI要約時のみ Anthropic に送信されます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("sk-ant-...") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}
