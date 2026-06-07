package com.yomiato.data.offline

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * オフライン保存の補助。
 * - [embedImages]: 本文中の画像をダウンロードして data URI に埋め込み、通信なしで表示できる HTML を返す。
 * - [snapshotFile] / [deleteArtifacts]: MHTML スナップショット（WebView.saveWebArchive）用のファイル管理。
 */
@Singleton
class OfflineArchiver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    /** 本文 HTML の画像を data URI 化する。失敗した画像は元のまま残す。 */
    suspend fun embedImages(html: String): String = withContext(Dispatchers.IO) {
        val doc = Jsoup.parse(html)
        var total = 0L
        for (img in doc.select("img")) {
            val src = img.attr("src")
            if (!src.startsWith("http", ignoreCase = true)) continue
            val image = runCatching { download(src) }.getOrNull() ?: continue
            if (image.bytes.size > MAX_IMAGE_BYTES) continue
            if (total + image.bytes.size > MAX_TOTAL_BYTES) break
            total += image.bytes.size
            val b64 = Base64.encodeToString(image.bytes, Base64.NO_WRAP)
            img.attr("src", "data:${image.mime};base64,$b64")
            img.removeAttr("srcset")
        }
        doc.body().html()
    }

    /** スナップショット（.mht）の保存先ファイル。 */
    fun snapshotFile(articleId: Long): File {
        val dir = File(context.filesDir, "snapshots").apply { mkdirs() }
        return File(dir, "$articleId.mht")
    }

    /** 記事削除時にオフライン成果物（スナップショット）を片付ける。 */
    fun deleteArtifacts(articleId: Long) {
        runCatching { snapshotFile(articleId).delete() }
    }

    private data class DownloadedImage(val bytes: ByteArray, val mime: String)

    private fun download(url: String): DownloadedImage? {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            val mime = response.header("Content-Type")?.substringBefore(";")?.trim()
                ?.takeIf { it.startsWith("image/") }
                ?: mimeFromUrl(url)
            return DownloadedImage(body.bytes(), mime)
        }
    }

    private fun mimeFromUrl(url: String): String = when {
        url.endsWith(".png", true) -> "image/png"
        url.endsWith(".gif", true) -> "image/gif"
        url.endsWith(".webp", true) -> "image/webp"
        url.endsWith(".svg", true) -> "image/svg+xml"
        else -> "image/jpeg"
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 1_500_000      // 画像1枚あたり上限 ~1.5MB
        const val MAX_TOTAL_BYTES = 8_000_000L     // 1記事あたり合計上限 ~8MB
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0 Safari/537.36"
    }
}
