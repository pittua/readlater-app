package com.yomiato.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** AI 要約＋タグ提案の結果。 */
data class AiResult(
    val summary: String,
    val tags: List<String>,
)

class AiException(message: String) : Exception(message)

/**
 * Anthropic Messages API を直接呼び、記事の日本語要約とタグ提案を取得する。
 * API キーはユーザー自身のもの（[SecureKeyStore]）を使う（BYO key）。
 */
@Singleton
class AiClient @Inject constructor(
    private val client: OkHttpClient,
    private val keyStore: SecureKeyStore,
) {
    suspend fun summarizeAndTag(
        title: String,
        text: String,
        existingTags: List<String>,
    ): AiResult = withContext(Dispatchers.IO) {
        val apiKey = keyStore.anthropicApiKey
            ?: throw AiException("API キーが未設定です")

        val userContent = buildString {
            append("タイトル: ").append(title).append("\n\n")
            if (existingTags.isNotEmpty()) {
                append("既存タグ（できるだけ流用）: ").append(existingTags.joinToString(", ")).append("\n\n")
            }
            append("本文:\n").append(text)
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put(
                "system",
                JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", SYSTEM_PROMPT)
                        .put("cache_control", JSONObject().put("type", "ephemeral")),
                ),
            )
            put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", userContent),
                ),
            )
        }.toString()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val responseText = client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiException(parseErrorMessage(raw, response.code))
            }
            raw
        }

        parseResult(responseText)
    }

    private fun parseResult(responseText: String): AiResult {
        val root = JSONObject(responseText)
        val content = root.optJSONArray("content")
            ?: throw AiException("応答の解析に失敗しました")
        val textBlock = (0 until content.length())
            .map { content.getJSONObject(it) }
            .firstOrNull { it.optString("type") == "text" }
            ?.optString("text")
            ?: throw AiException("応答にテキストがありません")

        val json = extractJsonObject(textBlock) ?: throw AiException("要約結果を解析できませんでした")
        val summary = json.optString("summary").trim()
        val tagsArray = json.optJSONArray("tags")
        val tags = buildList {
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tagsArray.optString(i).trim().takeIf { it.isNotEmpty() }?.let { add(it) }
                }
            }
        }
        if (summary.isEmpty()) throw AiException("要約が空でした")
        return AiResult(summary = summary, tags = tags)
    }

    /** モデルが ```json``` フェンスや前後文を付けても拾えるよう、最初の { … } を抽出する。 */
    private fun extractJsonObject(text: String): JSONObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun parseErrorMessage(raw: String, code: Int): String {
        val msg = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when {
            !msg.isNullOrBlank() -> "AI エラー: $msg"
            code == 401 -> "API キーが無効です"
            code == 429 -> "レート制限です。しばらく待って再試行してください"
            else -> "AI エラー (HTTP $code)"
        }
    }

    companion object {
        const val MODEL = "claude-haiku-4-5-20251001"

        private const val SYSTEM_PROMPT =
            "あなたは日本語のニュース・ブログ記事を整理する編集アシスタントです。" +
                "与えられた記事を読み、日本語で3〜5文の簡潔な要約を作り、内容を表すタグを最大5個提案してください。" +
                "タグは短い名詞（1〜2語）にし、既存タグがあれば優先的に流用してください。" +
                "出力は次の JSON のみとし、前後に説明やコードフェンスを付けないでください: " +
                "{\"summary\":\"...\",\"tags\":[\"tag1\",\"tag2\"]}"
    }
}
