package com.yomiato.ui.organize

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.data.local.relation.TagWithCount
import com.yomiato.ui.components.EmptyState

/** S-5: タグ一覧（チップ表示）。タップで絞り込み一覧へ、長押しで削除。 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
@Composable
fun TagsScreen(
    onOpenTag: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var tagToDelete by remember { mutableStateOf<TagWithCount?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("タグ") }) },
    ) { innerPadding ->
        if (tags.isEmpty()) {
            EmptyState(
                title = "タグがありません",
                message = "記事の長押しメニューからタグを付けると、ここに表示されます。",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { twc ->
                    TagChip(
                        label = "#${twc.tag.name}  ${twc.articleCount}",
                        onClick = { onOpenTag(twc.tag.id, twc.tag.name) },
                        onLongClick = { tagToDelete = twc },
                    )
                }
            }
        }
    }

    tagToDelete?.let { twc ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("タグを削除") },
            text = {
                Text("「#${twc.tag.name}」を削除します。${twc.articleCount} 件の記事からこのタグが外れます（記事自体は削除されません）。")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(twc.tag.id)
                    tagToDelete = null
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) { Text("キャンセル") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
