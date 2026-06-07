# 03. アーキテクチャ / 技術選定

## 3.1 全体方針

- **MVVM + 単方向データフロー（UDF）**。クリーンアーキテクチャの簡易版として、
  `UI(Compose) → ViewModel → Repository → DataSource(Room / Network)` の層構成にする。
- 状態は ViewModel が `StateFlow<UiState>` で公開し、Compose が購読して描画する。
- 個人開発のため過剰な抽象化は避け、レイヤは「UI / domain（任意） / data」の 3 つに留める。

```
┌─────────────────────────────────────────────┐
│ UI 層 (Jetpack Compose)                       │
│  Screen Composable ── collectAsState ──┐      │
│                                        ▼      │
│  ViewModel (StateFlow<UiState>, イベント処理)  │
└───────────────┬───────────────────────────────┘
                │ (suspend / Flow)
┌───────────────▼───────────────────────────────┐
│ data 層                                        │
│  ArticleRepository                             │
│   ├─ Room (ArticleDao, TagDao, FolderDao)      │
│   └─ Network (記事取得: OkHttp + 本文抽出)      │
│  WorkManager (バックグラウンド取得ワーカー)      │
└────────────────────────────────────────────────┘
```

## 3.2 技術スタック（推奨構成）

| 領域 | 採用 | 理由 |
| --- | --- | --- |
| 言語 | **Kotlin** | Android 標準・Coroutines/Flow が自然 |
| UI | **Jetpack Compose + Material 3** | 宣言的 UI。状態駆動で UDF と相性良 |
| 非同期 | **Kotlin Coroutines + Flow** | DB 監視・バックグラウンド処理 |
| DI | **Hilt** | ViewModel/Repository の注入を簡潔に |
| 永続化 | **Room** | SQLite ラッパ。Flow 連携・型安全・将来 FTS 拡張可 |
| 設定保存 | **DataStore (Preferences)** | テーマ・フォントサイズ等の軽量設定 |
| 通信 | **OkHttp**（＋必要なら Retrofit） | HTML 取得。API は無いので OkHttp 直で十分 |
| HTML 解析 | **Jsoup** | OGP/メタ抽出・DOM 操作 |
| 本文抽出 | **Readability4J**（or crux 等） | Readability 系の本文抽出。Jsoup と併用 |
| 画像 | **Coil** | Compose 対応の画像ローダ（サムネイル） |
| バックグラウンド | **WorkManager** | オフライン時の再取得・保存後の非同期取得 |
| ナビゲーション | **Navigation Compose** | 画面遷移 |
| テスト | JUnit / Turbine / Compose UI Test / Room in-memory | 単体・UI テスト |

> 代替案メモ: 本文抽出をアプリ内に持たず外部 API（Mercury/Reader API 等）に委ねる手もあるが、
> 運用コスト・プライバシー・オフライン性を考えローカル抽出（Readability4J）を第一候補とする。

## 3.3 モジュール / パッケージ構成（単一モジュール想定）

```
app/
 └─ src/main/java/com/example/yomiato/
    ├─ MainActivity.kt
    ├─ YomiatoApp.kt              # Application (Hilt)
    ├─ di/                        # Hilt モジュール
    ├─ data/
    │   ├─ local/                 # Room: AppDatabase, Dao, Entity
    │   ├─ remote/                # OkHttp クライアント, 記事フェッチャ
    │   ├─ extract/               # Jsoup + Readability4J で本文抽出
    │   ├─ repository/            # ArticleRepository 実装
    │   └─ work/                  # FetchArticleWorker (WorkManager)
    ├─ domain/                    # model（UI 用モデル）, ユースケース（任意・薄く）
    └─ ui/
        ├─ list/                  # 一覧画面 + ViewModel
        ├─ reader/                # 記事詳細(リーダー) + ViewModel
        ├─ organize/              # タグ/フォルダ管理
        ├─ search/                # 検索
        ├─ settings/              # 設定
        ├─ components/            # 共通 Composable
        └─ theme/                 # Material3 テーマ
```

※ MVP は単一 Gradle モジュールで開始。規模が増えたら `core / feature` へ分割を検討。

## 3.4 主要フロー

### 保存フロー（共有シート）
1. `ShareReceiverActivity`（`ACTION_SEND` インテントフィルタ）が URL を受信。
2. URL 正規化 → 重複チェック → `Article(status=PENDING)` を Room に insert。
3. 完了 UI を出して即終了（共有元に戻す）。
4. `FetchArticleWorker` をエンキュー → OkHttp で HTML 取得 → Jsoup/Readability4J で解析 →
   `Article` を `SUCCESS`/`FAILED` で update。
5. Room の Flow により一覧 UI が自動更新。

### 閲覧フロー
1. 一覧 ViewModel が `ArticleDao` の `Flow<List<Article>>` を購読し UiState 化。
2. 記事タップ → 詳細(リーダー) → 抽出本文を表示。自動既読更新。

## 3.5 権限 / マニフェスト要点

- `INTERNET`（記事取得）。
- 共有受信用 `intent-filter`（`ACTION_SEND`, `text/plain`）。
- 画像はネットワーク取得のみ（ストレージ権限不要）。

## 3.6 ビルド / 環境

- Android Studio（最新安定版）、Gradle Kotlin DSL（`build.gradle.kts`）。
- `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`。
- バージョンカタログ（`libs.versions.toml`）で依存を一元管理。

## 3.7 テスト方針（MVP）

- **Repository / 抽出ロジック**: 単体テスト（既知 HTML を食わせて抽出結果を検証）。
- **DAO**: in-memory Room でクエリ検証。
- **ViewModel**: Flow を Turbine で検証。
- **UI**: 主要画面のスモークテスト（一覧表示・保存・タグ付け）。
