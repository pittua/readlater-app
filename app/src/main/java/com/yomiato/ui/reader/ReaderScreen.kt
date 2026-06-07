package com.yomiato.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomiato.data.local.ExtractionStatus
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.settings.AppSettings

/** S-2: 記事詳細（リーダービュー）。抽出本文 / オフライン本文 / スナップショットを切り替えて表示。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val article by viewModel.article.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
                        article?.article?.siteName
                            ?: article?.article?.domain
                            ?: "記事",
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
                        IconButton(onClick = { viewModel.summarize() }) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "AI 要約",
                                tint = if (a.summary != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (a.contentHtml != null) {
                            IconButton(onClick = { viewModel.saveOffline() }) {
                                Icon(
                                    Icons.Filled.DownloadForOffline,
                                    contentDescription = "オフライン保存",
                                    tint = if (a.offlineContentHtml != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { openUrl(context, a.url) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "元のページを開く")
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
        val a = article
        val art = a?.article
        val busy by viewModel.busy.collectAsStateWithLifecycle()
        val suggestedTags by viewModel.suggestedTags.collectAsStateWithLifecycle()

        Column(modifier = Modifier.padding(innerPadding)) {
            if (busy) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (suggestedTags.isNotEmpty()) {
                SuggestedTagsBar(
                    tags = suggestedTags,
                    onAdd = viewModel::applySuggestedTag,
                    onDismiss = viewModel::dismissSuggestion,
                )
            }
            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                ReaderBody(
                    a = a,
                    art = art,
                    settings = settings,
                    context = context,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ReaderBody(
    a: ArticleWithTags?,
    art: com.yomiato.data.local.entity.ArticleEntity?,
    settings: AppSettings,
    context: android.content.Context,
    viewModel: ReaderViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            a == null || art == null -> LoadingOrEmpty(Modifier)

            // オフライン本文（画像埋め込み済み）を最優先
            art.offlineContentHtml != null -> ReaderContent(
                article = a,
                contentHtml = art.offlineContentHtml!!,
                settings = settings,
                modifier = Modifier.fillMaxSize(),
            )

            // 抽出本文
            art.contentHtml != null -> ReaderContent(
                article = a,
                contentHtml = art.contentHtml!!,
                settings = settings,
                modifier = Modifier,
            )

            // 本文なし・取得中
            art.extractionStatus == ExtractionStatus.PENDING && art.snapshotPath == null ->
                PendingView(Modifier.fillMaxSize())

            // スナップショット（MHTML）があればオフライン表示
            art.snapshotPath != null -> SnapshotView(
                path = art.snapshotPath!!,
                modifier = Modifier,
            )

            // 本文抽出に失敗：アプリ内 WebView で元ページ表示＋スナップショット保存
            else -> FailedView(
                url = art.url,
                title = art.title,
                snapshotPath = viewModel.snapshotPath(),
                onSnapshotSaved = viewModel::onSnapshotSaved,
                onOpenOriginal = { openUrl(context, art.url) },
                onRetry = viewModel::retry,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun ReaderContent(
    article: ArticleWithTags,
    contentHtml: String,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val html = remember(article.article.id, contentHtml, article.article.summary, settings, dark) {
        buildReaderHtml(
            title = article.article.title,
            siteName = article.article.siteName ?: article.article.domain,
            readMinutes = article.article.estimatedReadMinutes,
            contentHtml = contentHtml,
            fontScale = settings.readerFontScale,
            lineHeightScale = settings.readerLineHeightScale,
            dark = dark,
            summary = article.article.summary,
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).also { web ->
                web.settings.javaScriptEnabled = false
                web.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(article.article.url, html, "text/html", "utf-8", null)
        },
    )
}

/** MHTML スナップショットを WebView で表示（通信不要）。 */
@Composable
private fun SnapshotView(path: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).also { web ->
                web.settings.allowFileAccess = true
                web.settings.javaScriptEnabled = false
                web.webViewClient = WebViewClient()
                web.loadUrl("file://$path")
            }
        },
    )
}

@Composable
private fun PendingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text("本文を取得しています…", modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun LoadingOrEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 本文抽出に失敗した記事（x.com など JS 描画サイト）向け。
 * アプリ内 WebView で元ページを表示し、その場でスナップショット保存できる。
 */
@Composable
private fun FailedView(
    url: String,
    title: String?,
    snapshotPath: String,
    onSnapshotSaved: () -> Unit,
    onOpenOriginal: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "本文を抽出できないページです。元ページを表示しています。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    webViewRef?.saveWebArchive(snapshotPath, false) { result ->
                        if (result != null) onSnapshotSaved()
                    }
                },
            ) { Text("オフライン保存") }
            OutlinedButton(onClick = onRetry) { Text("再取得") }
            OutlinedButton(onClick = onOpenOriginal) { Text("ブラウザで開く") }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                    webViewRef = this
                }
            },
        )
    }
}

/** AI が提案したタグをワンタップで付与できるバー。 */
@Composable
private fun SuggestedTagsBar(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onDismiss: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            "AI 提案タグ（タップで追加）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onAdd(tag) },
                    label = { Text("#$tag") },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "候補を消す",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDismiss(tag) },
                        )
                    },
                )
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
