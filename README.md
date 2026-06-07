<div align="center">

# 📖 Yomiato（読みあと）

**「あとで読む」を、ちゃんと読む。**

気になった記事を 1 タップで集約保存し、AI 要約で素早く把握、タグ・フォルダ・検索で整理する Android 製の read-it-later アプリ。
保存はオフラインで読め、データは端末内のみ。

<br/>

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/DB-Room-FF6F00)
![Hilt](https://img.shields.io/badge/DI-Hilt-2196F3)

### ⬇️ [最新版 APK をダウンロード（v0.1.0）](https://github.com/pittua/readlater-app/releases/latest)

</div>

---

## ✨ できること

| | 機能 |
| :--: | --- |
| 📥 | **保存** — 他アプリの共有シート（`ACTION_SEND`）や URL 貼り付けで保存。タイトル・サムネ・本文をバックグラウンドで自動取得（重複は自動でまとめる） |
| 🌐 | **読む** — 記事はアプリ内 Web 表示で元ページをそのまま閲覧。読み込めない時は保存済みスナップショットに自動フォールバック |
| 🤖 | **AI 要約** — ワンタップで日本語要約＋タグ提案。`端末内（無料・オフライン）` と `Claude API（高品質）` を切替可能 |
| 🏷️ | **整理** — 複数タグ（サジェスト付き）・フォルダ振り分け・キーワード検索（タイトル/本文/ドメイン） |
| 👆 | **スワイプ操作** — 左スワイプでアーカイブ、右スワイプで削除（確認付き） |
| 📴 | **オフライン保存** — ページを MHTML スナップショットで丸ごと保存し、通信なしで再読 |
| 🎨 | **テーマ** — ライト / ダーク / システム追従、Android 12+ のダイナミックカラー対応 |

> 🔒 **プライバシー** — 記事データは端末内（Room）のみに保存。外部送信は「記事取得時の対象サイトアクセス」と、AI 要約に **Claude API を選んだ場合のみ**本文を送信します（既定の端末内エンジンは完全オフライン）。API キーは端末内に暗号化保存されます。

---

## 🧠 AI 要約の 2 つのエンジン

| | 端末内（既定） | Claude API（任意） |
| --- | --- | --- |
| 方式 | 形態素解析（kuromoji）＋ 重要文抽出（LexRank 風） | Claude Haiku による抽象要約 |
| 品質 | 本文から重要文を抜粋（誤要約なし） | 流暢な要約文 |
| コスト / 通信 | 無料・オフライン・全端末 | 要 API キー・通信あり |
| タグ | 頻度ベースのキーワード抽出 | 文脈を踏まえた提案 |

「API のレート制限・課金を気にせず使いたい」を既定で満たしつつ、品質が欲しい時だけ API に切り替えられます。

---

## 🛠 技術スタック

| 領域 | 採用技術 |
| --- | --- |
| 言語 / UI | Kotlin・Jetpack Compose・Material 3 |
| アーキテクチャ | MVVM + 単方向データフロー（UI → ViewModel → Repository → Room / Network） |
| 非同期 | Coroutines・Flow |
| DI | Hilt |
| 永続化 | Room（FTS 拡張も視野）・DataStore（設定）・EncryptedSharedPreferences（API キー） |
| 取得 / 解析 | OkHttp・Jsoup（OGP/メタ）・Readability4J（本文抽出） |
| バックグラウンド | WorkManager（オフライン復帰で自動リトライ） |
| 画像 | Coil |
| 端末内 NLP | kuromoji（ipadic） |

---

## 🚀 ビルド & 実行

```bash
# 必要: Android SDK / JDK 17+
git clone https://github.com/pittua/readlater-app.git
cd readlater-app
./gradlew :app:assembleDebug
# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

`compileSdk 35` / `minSdk 26` / `targetSdk 35`。依存はバージョンカタログ（`gradle/libs.versions.toml`）で一元管理。

---

## 🗂 アーキテクチャ概要

```
UI 層 (Jetpack Compose)
  Screen ── collectAsState ──▶ ViewModel (StateFlow<UiState>)
                                   │ suspend / Flow
data 層
  ArticleRepository
   ├─ Room (Article / Tag / Folder + 中間テーブル)
   ├─ Network (OkHttp + Jsoup + Readability4J)
   ├─ AI (LocalSummarizer / Claude AiClient)
   └─ Offline (画像埋め込み・MHTML スナップショット)
  WorkManager (FetchArticleWorker)
```

詳しい設計は [`docs/`](docs/) を参照：
[要件](docs/01-requirements.md)・[機能仕様](docs/02-specification.md)・[アーキテクチャ](docs/03-architecture.md)・[データモデル](docs/04-data-model.md)・[画面設計](docs/05-ui-screens.md)・[ロードマップ](docs/06-roadmap.md)

---

## 🧭 ロードマップ（MVP 後）

- [ ] ハイライト・メモ（注釈）
- [ ] 全文検索（Room FTS）
- [ ] エクスポート / インポート → 将来のバックアップ・同期
- [ ] RSS / ニュースレター取り込み、読み上げ（TTS）

---

<div align="center">
個人開発・ローカル完結を志向した read-it-later アプリです 📚
</div>
