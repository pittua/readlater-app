# Yomiato（読みあと）

「あとで読む」記事をスマホで保存・整理するための Android アプリ。
Readwise Reader を参考イメージとした個人向けリードイットレーター。

## コンセプト

気になった Web 記事を URL でサッと保存し、余計な要素を除いた読みやすいビューで読み、
タグ／フォルダで整理する。「保存して放置」になりがちな “あとで読む” を、
ちゃんと読める・見つけられる状態に保つことを目指す。

## ターゲット / 技術スタック

- **対象端末**: Android ネイティブ（最小 API 26 / Android 8.0 〜、ターゲット API 35）
- **言語 / UI**: Kotlin + Jetpack Compose
- **アーキテクチャ**: MVVM + 単方向データフロー（クリーンアーキテクチャの簡易版）
- 詳細は [docs/03-architecture.md](docs/03-architecture.md) を参照

## MVP スコープ

1. **URL 保存 / 記事取り込み** — 共有シートや URL 貼り付けから記事を保存し、本文を抽出
2. **タグ / フォルダ整理** — 保存記事をタグ・フォルダで分類・検索

MVP に含めない機能（将来候補）は [docs/01-requirements.md](docs/01-requirements.md) を参照。

## ドキュメント

| ファイル | 内容 |
| --- | --- |
| [docs/01-requirements.md](docs/01-requirements.md) | 要件定義（背景・ユーザー・スコープ） |
| [docs/02-specification.md](docs/02-specification.md) | 機能仕様（各機能の振る舞い） |
| [docs/03-architecture.md](docs/03-architecture.md) | アーキテクチャ / 技術選定 |
| [docs/04-data-model.md](docs/04-data-model.md) | データモデル（Room スキーマ） |
| [docs/05-ui-screens.md](docs/05-ui-screens.md) | 画面設計 / 画面遷移 |
| [docs/06-roadmap.md](docs/06-roadmap.md) | 開発ロードマップ（マイルストーン） |

## ステータス

MVP 実装完了（M1〜M4）。実機（Android 15 / API 35）で動作確認済み。

- 保存: 共有シート（`ACTION_SEND`）/ アプリ内 URL 追加 → WorkManager で本文・メタ取得（OkHttp + Jsoup + Readability4J）
- 閲覧: 受信トレイ（受信/既読/アーカイブ）・リーダービュー（WebView、設定でフォント/行間/テーマ調整）・自動既読
- 整理: タグ（複数・サジェスト）/ フォルダ（1 記事 1 フォルダ）/ キーワード検索（LIKE）
- 設定: テーマ（ライト/ダーク/システム）・ダイナミックカラー・本文フォント/行間・自動既読・全削除（DataStore 永続化）

ビルド: `./gradlew :app:assembleDebug`（要 Android SDK / JDK 17+）。
