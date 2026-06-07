package com.yomiato.data.settings

/** テーマの適用方式。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** 要約・タグ生成のエンジン。 */
enum class SummaryEngine {
    /** 端末内の形態素解析による抽出要約＋キーワードタグ（無料・オフライン・全端末）。 */
    LOCAL,

    /** Claude API による抽象要約＋タグ（高品質・キー必要・本文を外部送信）。 */
    CLOUD,
}

/**
 * ユーザー設定のスナップショット。リーダービューの表示とアプリ全体のテーマに反映する。
 *
 * @param readerFontScale 本文フォント倍率（1.0 が標準）。
 * @param readerLineHeightScale 本文行間倍率（1.0 が標準）。
 * @param autoMarkRead 記事を開いたら自動で既読にするか。
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val readerFontScale: Float = 1.0f,
    val readerLineHeightScale: Float = 1.0f,
    val autoMarkRead: Boolean = true,
    val summaryEngine: SummaryEngine = SummaryEngine.LOCAL,
)
