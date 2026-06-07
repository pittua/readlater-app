package com.yomiato.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/** S-2: 記事詳細（リーダービュー）。抽出本文を WebView で読みやすく表示。 */
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

    LaunchedEffect(article?.article?.id) {
        if (article != null) viewModel.onOpened()
    }

    Scaffold(
        modifier = modifier,
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
        when {
            a == null -> LoadingOrEmpty(Modifier.padding(innerPadding))
            a.article.extractionStatus == ExtractionStatus.PENDING && a.article.contentHtml == null ->
                PendingView(Modifier.padding(innerPadding))
            a.article.contentHtml.isNullOrBlank() ->
                FailedView(
                    article = a,
                    modifier = Modifier.padding(innerPadding),
                    onOpenOriginal = { openUrl(context, a.article.url) },
                    onRetry = viewModel::retry,
                )
            else -> ReaderContent(
                article = a,
                settings = settings,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ReaderContent(
    article: ArticleWithTags,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val html = remember(article.article.id, article.article.contentHtml, settings, dark) {
        buildReaderHtml(
            title = article.article.title,
            siteName = article.article.siteName ?: article.article.domain,
            readMinutes = article.article.estimatedReadMinutes,
            contentHtml = article.article.contentHtml.orEmpty(),
            fontScale = settings.readerFontScale,
            lineHeightScale = settings.readerLineHeightScale,
            dark = dark,
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

@Composable
private fun FailedView(
    article: ArticleWithTags,
    onOpenOriginal: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            article.article.title ?: article.article.url,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "この記事は本文を取得できませんでした。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(onClick = onOpenOriginal, modifier = Modifier.padding(top = 24.dp)) {
            Text("元のページを開く")
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text("再取得する")
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
