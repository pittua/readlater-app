package com.yomiato.data.ai

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * 端末内（API 不要・オフライン）の要約・タグ生成。
 * - 要約: 形態素解析でキーワードを取り、文どうしの類似度から重要文を抜き出す抽出型（LexRank 風）。
 *   本文の文をそのまま抜粋するため、誤情報（ハルシネーション）は出ない。
 * - タグ: 名詞・英単語の頻度上位を抽出し、既存タグを優先して提案する。
 */
@Singleton
class LocalSummarizer @Inject constructor() {

    private val tokenizer by lazy { Tokenizer() }

    data class Result(val summary: String, val tags: List<String>)

    suspend fun summarizeAndTag(
        title: String,
        text: String,
        existingTags: List<String>,
        maxSentences: Int = 3,
        maxTags: Int = 5,
    ): Result = withContext(Dispatchers.Default) {
        val clean = text.replace("\r", "\n").trim()
        val sentences = splitSentences(clean)
        val summary = if (sentences.size <= maxSentences) {
            sentences.joinToString("").ifBlank { clean.take(200) }
        } else {
            rankSentences(sentences, maxSentences)
        }
        val tags = extractTags(title, clean, existingTags, maxTags)
        Result(summary = summary.trim().take(600), tags = tags)
    }

    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[。．！？!?])|\\n+"))
            .map { it.trim() }
            .filter { isProse(it) }

    /** 散文らしい文だけを残す（コード・引用定型文・記号過多・短すぎ/長すぎを除外）。 */
    private fun isProse(s: String): Boolean {
        if (s.length !in 12..300) return false
        if (s.contains("//")) return false
        // 波括弧・セミコロン・代入/比較記号は散文にほぼ出ず、コードの強い指標なので除外
        if (CODE_PATTERN.containsMatchIn(s)) return false
        if (NOISE_PATTERN.containsMatchIn(s)) return false
        val symbolRatio = s.count { it in "[]()<>|/\\\t#*" }.toDouble() / s.length
        if (symbolRatio > 0.06) return false
        val letterRatio = s.count { it.isLetter() }.toDouble() / s.length
        return letterRatio >= 0.5
    }

    private fun rankSentences(sentences: List<String>, k: Int): String {
        val vectors = sentences.map { tf(contentWords(it)) }
        val scores = DoubleArray(sentences.size)
        for (i in sentences.indices) {
            for (j in sentences.indices) {
                if (i != j) scores[i] += cosine(vectors[i], vectors[j])
            }
        }
        // リード文（先頭）は重要なことが多いので少し優遇する。
        if (scores.isNotEmpty()) scores[0] *= 1.1
        val top = scores.indices.sortedByDescending { scores[it] }.take(k).sorted()
        return top.joinToString("") { sentences[it] }
    }

    private fun extractTags(
        title: String,
        text: String,
        existing: List<String>,
        max: Int,
    ): List<String> {
        val counts = LinkedHashMap<String, Int>()
        fun add(raw: String, weight: Int) {
            val term = normalizeTerm(raw) ?: return
            counts[term] = (counts[term] ?: 0) + weight
        }
        tokenizer.tokenize(text).filter { keepForTag(it) }.forEach { add(baseOf(it), 1) }
        englishWords(text).forEach { add(it, 1) }
        // タイトル中の語は重み付けして上位に来やすくする。
        tokenizer.tokenize(title).filter { keepForTag(it) }.forEach { add(baseOf(it), 3) }
        englishWords(title).forEach { add(it, 3) }

        val ranked = counts.entries.sortedByDescending { it.value }.map { it.key }
        val existingLower = existing.map { it.lowercase(Locale.ROOT) }.toSet()
        val preferred = ranked.filter { it.lowercase(Locale.ROOT) in existingLower }
        val rest = ranked.filter { it.lowercase(Locale.ROOT) !in existingLower }
        return (preferred + rest).distinct().take(max)
    }

    private fun contentWords(s: String): List<String> {
        val jp = tokenizer.tokenize(s).filter { keepForSimilarity(it) }.map { baseOf(it) }
        return jp + englishWords(s)
    }

    /** タグ候補を正規化。ASCII語は小文字化＋英語ストップワード除外（kuromoji が英語を名詞化する分も弾く）。日本語はそのまま。 */
    private fun normalizeTerm(s: String): String? {
        val isAscii = s.all { it.code < 128 }
        if (!isAscii) return s.takeIf { it.length >= 2 }
        val low = s.lowercase(Locale.ROOT)
        return if (low.length < 3 || low in EN_STOP || !low.first().isLetter()) null else low
    }

    private fun englishWords(s: String): List<String> =
        Regex("[A-Za-z][A-Za-z0-9]{2,}").findAll(s)
            .map { it.value.lowercase(Locale.ROOT) }
            .filter { it !in EN_STOP }
            .toList()

    private fun keepForSimilarity(t: Token): Boolean {
        val p1 = t.partOfSpeechLevel1
        return (p1 == "名詞" || p1 == "動詞" || p1 == "形容詞") && surfaceOk(t)
    }

    private fun keepForTag(t: Token): Boolean {
        if (t.partOfSpeechLevel1 != "名詞") return false
        if (t.partOfSpeechLevel2 in NOUN_EXCLUDE) return false
        val base = baseOf(t)
        return surfaceOk(t) && base.length >= 2 && base !in JP_STOP
    }

    private fun surfaceOk(t: Token): Boolean {
        val s = t.surface
        return s.length >= 2 && !s.all { it.isDigit() } && !s.matches(Regex("[ぁ-ん]{1,2}"))
    }

    private fun baseOf(t: Token): String =
        t.baseForm?.takeIf { it != "*" && it.isNotBlank() } ?: t.surface

    private fun tf(words: List<String>): Map<String, Double> {
        val m = HashMap<String, Double>()
        words.forEach { m[it] = (m[it] ?: 0.0) + 1.0 }
        return m
    }

    private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val keys = if (a.size < b.size) a.keys else b.keys
        var dot = 0.0
        for (k in keys) {
            val av = a[k] ?: continue
            val bv = b[k] ?: continue
            dot += av * bv
        }
        val na = sqrt(a.values.sumOf { it * it })
        val nb = sqrt(b.values.sumOf { it * it })
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
    }

    private companion object {
        // コードの強い指標（波括弧・セミコロン・代入/比較/アロー）。散文にはほぼ出ない記号のみ。
        val CODE_PATTERN = Regex("[{};=]|->|::")
        // 引用・脚注・コード由来の定型ノイズ（要約から除外）
        val NOISE_PATTERN = Regex(
            "Retrieved|Archived|ISBN|doi:|\\[edit\\]|\\bcite\\b|\\bp\\.|\\bpp\\.|^\\^|https?://",
            RegexOption.IGNORE_CASE,
        )
        val NOUN_EXCLUDE = setOf("非自立", "代名詞", "数", "接尾", "副詞可能")
        val JP_STOP = setOf(
            "こと", "もの", "ため", "よう", "とき", "これ", "それ", "あれ",
            "ここ", "そこ", "場合", "とおり", "うち", "ほう", "など",
        )
        val EN_STOP = setOf(
            "the", "and", "for", "that", "this", "with", "from", "are", "was", "were",
            "have", "has", "had", "but", "not", "you", "your", "their", "they", "its",
            "into", "than", "then", "out", "about", "which", "while", "will", "can",
            "could", "would", "should", "may", "also", "such", "more", "most", "other",
            "some", "what", "when", "where", "who", "how", "all", "any", "one", "two",
            "new", "use", "using", "used", "been", "being", "there", "here",
            // 引用・Wiki・URL 由来のノイズ語
            "retrieved", "archived", "original", "edit", "isbn", "doi", "vol", "cite",
            "ref", "www", "com", "org", "net", "https", "http", "pdf", "html",
        )
    }
}
