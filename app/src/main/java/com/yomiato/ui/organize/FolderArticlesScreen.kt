package com.yomiato.ui.organize

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.ui.components.ArticleList

/** フォルダ内の記事一覧。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderArticlesScreen(
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderArticlesViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        ArticleList(
            items = articles,
            emptyTitle = "このフォルダは空です",
            emptyMessage = "記事の長押しメニューからこのフォルダへ移動できます。",
            onOpenArticle = onOpenArticle,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
