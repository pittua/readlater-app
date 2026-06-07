package com.yomiato.data.util

import java.util.Locale

/**
 * URL の抽出と正規化。正規化結果は重複判定（normalizedUrl のユニークキー）に使う。
 * 仕様: docs/02-specification.md F-1.4。
 */
object UrlUtils {

    private val URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    /** トラッキング用途のクエリパラメータ（正規化時に除去）。 */
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_reader", "utm_referrer",
        "fbclid", "gclid", "dclid", "gclsrc", "msclkid", "yclid",
        "mc_cid", "mc_eid", "igshid", "spm", "ref", "ref_src", "ref_url",
    )

    /** 共有テキストなどから最初の URL を抽出する。見つからなければ null。 */
    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return URL_REGEX.find(text)?.value?.trim()?.trimEnd('.', ',', ')', ']', '}')
    }

    /** URL として最低限妥当か（http/https かつホストを持つ）。 */
    fun isValidHttpUrl(raw: String): Boolean = runCatching {
        val uri = java.net.URI(raw.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    /** 表示用ホスト名（先頭の www. を除去）。取得できなければ null。 */
    fun host(raw: String): String? = runCatching {
        java.net.URI(raw.trim()).host?.removePrefix("www.")
    }.getOrNull()

    /**
     * 重複判定用の正規化:
     * - スキーム/ホストを小文字化
     * - 既定ポート除去
     * - トラッキングパラメータ除去、残りのクエリはキー順でソート
     * - フラグメント除去
     * - 末尾スラッシュ正規化
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return trimmed.lowercase(Locale.ROOT)

        val scheme = (uri.scheme ?: "https").lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT) ?: return trimmed.lowercase(Locale.ROOT)
        val port = when {
            uri.port == -1 -> ""
            scheme == "http" && uri.port == 80 -> ""
            scheme == "https" && uri.port == 443 -> ""
            else -> ":${uri.port}"
        }

        val path = (uri.path ?: "").let { p ->
            if (p.length > 1 && p.endsWith("/")) p.dropLast(1) else p
        }.ifEmpty { "" }

        val query = uri.query
            ?.split("&")
            ?.mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val key = part.substringBefore("=")
                if (key.lowercase(Locale.ROOT) in TRACKING_PARAMS) null else part
            }
            ?.sorted()
            ?.joinToString("&")
            ?.takeIf { it.isNotEmpty() }

        return buildString {
            append(scheme).append("://").append(host).append(port).append(path)
            if (query != null) append("?").append(query)
        }
    }
}
