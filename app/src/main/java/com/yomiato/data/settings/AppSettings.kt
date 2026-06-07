package com.yomiato.data.settings

/** テーマの適用方式。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
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
)
