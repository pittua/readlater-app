package com.yomiato.data.remote

import com.yomiato.data.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max

/** 記事取得・本文抽出の結果。 */
data class FetchResult(
    val title: String?,
    val siteName: String?,
    val domain: String?,
    val thumbnailUrl: String?,
    val excerpt: String?,
    val contentHtml: String?,
    val contentText: String?,
    val estimatedReadMinutes: Int?,
)

/**
 * URL から HTML を取得し、OGP メタ（Jsoup）と本文（Readability4J）を抽出する。
 * 仕様: docs/02-specification.md F-1.3。
 */
@Singleton
class ArticleFetcher @Inject constructor(
    private val client: OkHttpClient,
) {
    /**
     * 取得・抽出を実行。ネットワーク/パース失敗時は例外を投げる（呼び出し側で FAILED 扱い）。
     * メタは取れたが本文抽出に失敗した場合は contentHtml=null で返す（部分成功）。
     */
    suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "ja,en;q=0.8")
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("empty body")
            body.string()
        }

        val doc = Jsoup.parse(html, url)
        val domain = UrlUtils.host(url)

        // --- OGP / メタ ---
        val ogTitle = doc.metaContent("og:title")
        val htmlTitle = doc.title().ifBlank { null }
        val title = ogTitle ?: htmlTitle
        val siteName = doc.metaContent("og:site_name") ?: domain
        val thumbnail = doc.metaContent("og:image")?.let { absolute(doc, it) }
        val ogDescription = doc.metaContent("og:description")
            ?: doc.metaContent("description", isName = true)

        // --- 本文抽出（Readability4J） ---
        val article = runCatching { Readability4J(url, html).parse() }.getOrNull()
        val contentHtml = article?.content?.takeIf { it.isNotBlank() }
        val contentText = article?.textContent?.trim()?.takeIf { it.isNotBlank() }

        val excerpt = (ogDescription ?: article?.excerpt ?: contentText)
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(200)

        val readMinutes = contentText?.let { estimateReadMinutes(it) }

        FetchResult(
            title = title?.trim(),
            siteName = siteName?.trim(),
            domain = domain,
            thumbnailUrl = thumbnail,
            excerpt = excerpt,
            contentHtml = contentHtml,
            contentText = contentText,
            estimatedReadMinutes = readMinutes,
        )
    }

    private fun Document.metaContent(key: String, isName: Boolean = false): String? {
        val selector = if (isName) "meta[name=$key]" else "meta[property=$key]"
        return selectFirst(selector)?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun absolute(doc: Document, ref: String): String =
        runCatching { doc.baseUri().let { java.net.URI(it).resolve(ref).toString() } }
            .getOrDefault(ref)

    /** 文字数ベースの推定読了時間（日本語想定 500 字/分、最低 1 分）。 */
    private fun estimateReadMinutes(text: String): Int =
        max(1, ceil(text.length / CHARS_PER_MINUTE.toDouble()).toInt())

    companion object {
        private const val CHARS_PER_MINUTE = 500
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0 Safari/537.36"
    }
}
