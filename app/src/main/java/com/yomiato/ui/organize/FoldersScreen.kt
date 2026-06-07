package com.yomiato.ui.organize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.data.local.entity.FolderEntity
import com.yomiato.ui.components.EmptyState

/** S-4: フォルダ一覧。作成・リネーム・削除と、フォルダ内記事への遷移。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onOpenFolder: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("フォルダ") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "フォルダを作成")
            }
        },
    ) { innerPadding ->
        if (folders.isEmpty()) {
            EmptyState(
                title = "フォルダがありません",
                message = "＋ からフォルダを作成して記事を整理しましょう。",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(folders, key = { it.folder.id }) { fwc ->
                    var menuOpen by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(fwc.folder.name) },
                        supportingContent = { Text("${fwc.articleCount} 件") },
                        leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "メニュー")
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("名前を変更") },
                                        onClick = { menuOpen = false; renameTarget = fwc.folder },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("削除") },
                                        onClick = { menuOpen = false; viewModel.delete(fwc.folder.id) },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenFolder(fwc.folder.id, fwc.folder.name) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        FolderNameDialog(
            title = "フォルダを作成",
            initial = "",
            onConfirm = { viewModel.create(it); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }

    renameTarget?.let { folder ->
        FolderNameDialog(
            title = "名前を変更",
            initial = folder.name,
            onConfirm = { viewModel.rename(folder.id, it); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("フォルダ名") },
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
