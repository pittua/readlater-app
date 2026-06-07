package com.yomiato.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

/**
 * S-2: 記事詳細。基本はアプリ内 WebView で元ページをそのまま表示する。
 * 「読みやすく」する役割は AI 要約（上部の✨から要約シート）が担う。
 * オフライン保存したページ（MHTML スナップショット）は通信エラー時に自動で表示される。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val article by viewModel.article.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showSummarySheet by remember { mutableStateOf(false) }

    LaunchedEffect(article?.article?.id) {
        if (article != null) viewModel.onOpened()
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        article?.article?.siteName ?: article?.article?.domain ?: "記事",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    val a = article?.article
                    if (a != null) {
                        IconButton(onClick = { showSummarySheet = true }) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "AI 要約",
                                tint = if (a.summary != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            val webView = webViewRef
                            if (webView != null) {
                                webView.saveWebArchive(viewModel.snapshotPath(), false) { result ->
                                    if (result != null) viewModel.onSnapshotSaved()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.DownloadForOffline,
                                contentDescription = "オフライン保存",
                                tint = if (a.snapshotPath != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { openUrl(context, a.url) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "ブラウザで開く")
                        }
                        IconButton(onClick = { shareUrl(context, a.url, a.title) }) {
                            Icon(Icons.Filled.Share, contentDescription = "共有")
                        }
                        IconButton(onClick = { viewModel.toggleArchive() }) {
                            Icon(Icons.Filled.Archive, contentDescription = "アーカイブ")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val a = article?.article
            if (a == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                WebPage(
                    url = a.url,
                    snapshotPath = a.snapshotPath,
                    onWebViewReady = { webViewRef = it },
                )
            }
        }
    }

    if (showSummarySheet) {
        SummarySheet(
            summary = article?.article?.summary,
            summaryModel = article?.article?.summaryModel,
            viewModel = viewModel,
            onDismiss = { showSummarySheet = false },
        )
    }
}

/** 元ページの WebView。通信エラー時はオフライン保存済みスナップショットへ自動フォールバック。 */
@Composable
private fun WebPage(
    url: String,
    snapshotPath: String?,
    onWebViewReady: (WebView) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        // オフライン等でメインページ読み込みに失敗したら、保存済みスナップショットを表示
                        if (request?.isForMainFrame == true &&
                            !snapshotPath.isNullOrEmpty() &&
                            File(snapshotPath).exists()
                        ) {
                            view?.loadUrl("file://$snapshotPath")
                        }
                    }
                }
                loadUrl(url)
                onWebViewReady(this)
            }
        },
    )
}

/** AI 要約パネル（リーダーの代替）。要約の生成・表示と提案タグのワンタップ付与。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SummarySheet(
    summary: String?,
    summaryModel: String?,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val suggestedTags by viewModel.suggestedTags.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("AI 要約", style = MaterialTheme.typography.titleMedium)

            when {
                busy -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                    Text(
                        "要約しています…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                summary == null -> {
                    Text(
                        "この記事を要約します。エンジンは設定で切り替えられます（端末内 / Claude API）。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Button(
                        onClick = { viewModel.summarize() },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("要約する") }
                }

                else -> {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (!summaryModel.isNullOrBlank()) {
                        Text(
                            summaryModel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (suggestedTags.isNotEmpty()) {
                        Text(
                            "提案タグ（タップで追加）",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestedTags.forEach { tag ->
                                AssistChip(
                                    onClick = { viewModel.applySuggestedTag(tag) },
                                    label = { Text("#$tag") },
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.summarize() },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("再要約") }
                }
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun shareUrl(context: android.content.Context, url: String, title: String?) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, if (title.isNullOrBlank()) url else "$title\n$url")
    }
    runCatching { context.startActivity(Intent.createChooser(send, "共有")) }
}
