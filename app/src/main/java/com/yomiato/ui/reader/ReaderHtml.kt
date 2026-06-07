package com.yomiato.ui.reader

import com.yomiato.ui.components.formatReadMinutes

/**
 * 抽出本文を読みやすい HTML 文書に整形する。フォントサイズ・行間は設定倍率を反映し、
 * ダークモードでは配色を反転する。WebView の loadDataWithBaseURL に渡す。
 */
fun buildReaderHtml(
    title: String?,
    siteName: String?,
    readMinutes: Int?,
    contentHtml: String,
    fontScale: Float,
    lineHeightScale: Float,
    dark: Boolean,
    summary: String? = null,
): String {
    val basePx = 18
    val fontPx = (basePx * fontScale).coerceIn(12f, 36f)
    val lineHeight = (1.75f * lineHeightScale).coerceIn(1.2f, 2.6f)

    val textColor = if (dark) "#E3E3E3" else "#1A1A1A"
    val mutedColor = if (dark) "#9E9E9E" else "#666666"
    val bgColor = if (dark) "#121212" else "#FFFFFF"
    val linkColor = if (dark) "#80B7FF" else "#1F6FEB"
    val ruleColor = if (dark) "#333333" else "#E0E0E0"

    val meta = buildList {
        siteName?.takeIf { it.isNotBlank() }?.let { add(escapeHtml(it)) }
        formatReadMinutes(readMinutes)?.let { add(it) }
    }.joinToString(" ・ ")

    val accent = if (dark) "#80B7FF" else "#1F6FEB"
    val summaryBg = if (dark) "#15233A" else "#EAF2FF"

    val titleBlock = buildString {
        if (!title.isNullOrBlank()) {
            append("<h1 class=\"yomiato-title\">").append(escapeHtml(title)).append("</h1>")
        }
        if (meta.isNotEmpty()) {
            append("<div class=\"yomiato-meta\">").append(meta).append("</div>")
        }
        if (!summary.isNullOrBlank()) {
            append("<div class=\"yomiato-summary\">")
            append("<div class=\"yomiato-summary-label\">AI 要約</div>")
            append(escapeHtml(summary).replace("\n", "<br/>"))
            append("</div>")
        }
        if (meta.isNotEmpty() || !summary.isNullOrBlank()) {
            append("<hr class=\"yomiato-rule\"/>")
        }
    }

    return """
        <!DOCTYPE html>
        <html lang="ja">
        <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=5"/>
        <style>
          :root { color-scheme: ${if (dark) "dark" else "light"}; }
          body {
            margin: 0;
            padding: 20px 18px 64px 18px;
            font-size: ${fontPx}px;
            line-height: $lineHeight;
            color: $textColor;
            background: $bgColor;
            font-family: -apple-system, "Hiragino Kaku Gothic ProN", "Noto Sans JP", sans-serif;
            word-wrap: break-word;
            overflow-wrap: break-word;
          }
          .yomiato-title { font-size: 1.5em; line-height: 1.35; margin: 0 0 8px 0; }
          .yomiato-meta { color: $mutedColor; font-size: 0.8em; margin-bottom: 12px; }
          .yomiato-summary { background: $summaryBg; border-left: 3px solid $accent; border-radius: 8px; padding: 12px 14px; margin: 4px 0 16px 0; font-size: 0.95em; }
          .yomiato-summary-label { color: $accent; font-size: 0.75em; font-weight: bold; margin-bottom: 6px; letter-spacing: 0.04em; }
          .yomiato-rule { border: none; border-top: 1px solid $ruleColor; margin: 0 0 20px 0; }
          img, video, iframe { max-width: 100%; height: auto; border-radius: 6px; }
          a { color: $linkColor; }
          h1, h2, h3, h4 { line-height: 1.35; }
          pre, code { white-space: pre-wrap; word-break: break-word; background: ${if (dark) "#1E1E1E" else "#F5F5F5"}; border-radius: 4px; }
          pre { padding: 12px; overflow-x: auto; }
          blockquote { margin: 16px 0; padding-left: 12px; border-left: 3px solid $ruleColor; color: $mutedColor; }
          figure { margin: 16px 0; }
          table { max-width: 100%; display: block; overflow-x: auto; }
        </style>
        </head>
        <body>
        $titleBlock
        $contentHtml
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
