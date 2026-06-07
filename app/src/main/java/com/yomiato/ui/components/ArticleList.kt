package com.yomiato.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.relation.ArticleWithTags

/**
 * 記事一覧の共通描画。一覧表示・タップで詳細遷移・長押しで操作シート（既読/アーカイブ/
 * タグ/フォルダ/削除/元ページ）を提供する。操作は [ArticleActionsViewModel] に委譲。
 */
@Composable
fun ArticleList(
    items: List<ArticleWithTags>,
    emptyTitle: String,
    emptyMessage: String,
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    actionsViewModel: ArticleActionsViewModel = hiltViewModel(),
) {
    if (items.isEmpty()) {
        EmptyState(emptyTitle, emptyMessage, modifier.fillMaxSize())
        return
    }

    var selected by remember { mutableStateOf<ArticleEntity?>(null) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(items, key = { it.article.id }) { item ->
            ArticleRow(
                item = item,
                onClick = { onOpenArticle(item.article.id) },
                onLongClick = { selected = item.article },
                onRetry = { actionsViewModel.retry(item.article.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }

    selected?.let { article ->
        ArticleActionsSheet(
            article = article,
            viewModel = actionsViewModel,
            onDismiss = { selected = null },
            onOpenArticle = onOpenArticle,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleActionsSheet(
    article: ArticleEntity,
    viewModel: ArticleActionsViewModel,
    onDismiss: () -> Unit,
    onOpenArticle: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showTagDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = article.title?.takeIf { it.isNotBlank() } ?: article.url,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            HorizontalDivider()

            SheetAction(
                icon = if (article.isRead) Icons.Filled.MarkEmailUnread else Icons.Filled.MarkEmailRead,
                label = if (article.isRead) "未読にする" else "既読にする",
            ) {
                viewModel.toggleRead(article); onDismiss()
            }
            SheetAction(
                icon = if (article.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                label = if (article.isArchived) "アーカイブ解除" else "アーカイブ",
            ) {
                viewModel.toggleArchive(article); onDismiss()
            }
            SheetAction(icon = Icons.Filled.Sell, label = "タグを編集") { showTagDialog = true }
            SheetAction(icon = Icons.Filled.Folder, label = "フォルダへ移動") { showFolderDialog = true }
            SheetAction(icon = Icons.AutoMirrored.Filled.OpenInNew, label = "元のページを開く") {
                openUrl(context, article.url); onDismiss()
            }
            SheetAction(
                icon = Icons.Filled.Delete,
                label = "削除",
                tint = MaterialTheme.colorScheme.error,
            ) { showDeleteConfirm = true }
        }
    }

    if (showTagDialog) {
        TagEditDialog(
            articleId = article.id,
            viewModel = viewModel,
            onDismiss = { showTagDialog = false },
        )
    }

    if (showFolderDialog) {
        FolderPickDialog(
            currentFolderId = article.folderId,
            viewModel = viewModel,
            onPick = { folderId ->
                viewModel.moveToFolder(article.id, folderId)
                showFolderDialog = false
                onDismiss()
            },
            onDismiss = { showFolderDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("記事を削除") },
            text = { Text("この記事を完全に削除します。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(article.id)
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TagEditDialog(
    articleId: Long,
    viewModel: ArticleActionsViewModel,
    onDismiss: () -> Unit,
) {
    val currentTags by viewModel.tagsForArticle(articleId).collectAsStateWithLifecycle(emptyList())
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("タグを編集") },
        text = {
            Column {
                if (currentTags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.removeTag(articleId, tag.id) },
                                label = { Text("#${tag.name}") },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("タグを追加") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                val currentNames = currentTags.map { it.name }.toSet()
                val suggestions = allTags.filter {
                    it.name !in currentNames &&
                        (input.isBlank() || it.name.contains(input.trim(), ignoreCase = true))
                }.take(8)
                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        suggestions.forEach { tag ->
                            AssistChip(
                                onClick = { viewModel.addTag(articleId, tag.name) },
                                label = { Text("#${tag.name}") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = input.trim()
                if (name.isNotEmpty()) {
                    viewModel.addTag(articleId, name)
                    input = ""
                } else {
                    onDismiss()
                }
            }) { Text(if (input.isBlank()) "閉じる" else "追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("完了") }
        },
    )
}

@Composable
private fun FolderPickDialog(
    currentFolderId: Long?,
    viewModel: ArticleActionsViewModel,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("フォルダへ移動") },
        text = {
            Column {
                FolderRadioRow("受信トレイ（未分類）", currentFolderId == null) { onPick(null) }
                folders.forEach { folder ->
                    FolderRadioRow(folder.name, currentFolderId == folder.id) { onPick(folder.id) }
                }
                if (folders.isEmpty()) {
                    Text(
                        "フォルダがありません。フォルダタブで作成できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

@Composable
private fun FolderRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
