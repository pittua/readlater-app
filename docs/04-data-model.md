# 04. データモデル（Room スキーマ）

ローカル DB（Room / SQLite）のスキーマ定義。MVP のエンティティと関連を示す。

## 4.1 ER 概要

```
Folder 1 ──< Article >── M:N ──< ArticleTag >── M:N ── Tag

- Article は 0..1 個の Folder に属する（folderId が null = 受信トレイ）
- Article と Tag は多対多（中間テーブル ArticleTag）
```

## 4.2 エンティティ定義

### Article（記事）

| カラム | 型 | 説明 |
| --- | --- | --- |
| id | Long (PK, autoGenerate) | 内部 ID |
| url | String | 元 URL（生） |
| normalizedUrl | String (unique index) | 正規化 URL（重複判定キー） |
| title | String? | タイトル |
| siteName | String? | サイト名 |
| domain | String? | ホスト名（表示・検索用） |
| thumbnailUrl | String? | サムネイル画像 URL（og:image） |
| excerpt | String? | 抜粋（一覧表示用） |
| contentHtml | String? | 抽出本文（整形済み HTML） |
| contentText | String? | 抽出本文（プレーン、検索・文字数用） |
| estimatedReadMinutes | Int? | 推定読了時間（分） |
| folderId | Long? (FK→Folder, nullable, index) | 所属フォルダ。null=受信トレイ |
| isRead | Boolean (default false) | 既読フラグ |
| isArchived | Boolean (default false, index) | アーカイブ |
| isFavorite | Boolean (default false) | お気に入り（任意機能） |
| extractionStatus | Enum(PENDING/SUCCESS/FAILED) | 取得・抽出状態 |
| createdAt | Long (epoch millis, index) | 保存日時 |
| updatedAt | Long (epoch millis) | 更新日時 |

- インデックス: `normalizedUrl`(unique), `folderId`, `isArchived`, `createdAt`。
- `folderId` の外部キーは `onDelete = SET NULL`（フォルダ削除時は受信トレイへ）。

### Tag（タグ）

| カラム | 型 | 説明 |
| --- | --- | --- |
| id | Long (PK, autoGenerate) | 内部 ID |
| name | String (unique index) | タグ名（正規化済み） |
| createdAt | Long | 作成日時 |

### Folder（フォルダ）

| カラム | 型 | 説明 |
| --- | --- | --- |
| id | Long (PK, autoGenerate) | 内部 ID |
| name | String | フォルダ名 |
| sortOrder | Int | 並び順 |
| createdAt | Long | 作成日時 |

### ArticleTag（中間テーブル / 多対多）

| カラム | 型 | 説明 |
| --- | --- | --- |
| articleId | Long (FK→Article, onDelete CASCADE) | 複合 PK |
| tagId | Long (FK→Tag, onDelete CASCADE) | 複合 PK |

- 複合主キー `(articleId, tagId)`。両 FK に index。

## 4.3 DAO（主なクエリ）

### ArticleDao
- `observeInbox(): Flow<List<ArticleWithTags>>` — `isArchived=0` を `createdAt DESC`。
- `observeByFolder(folderId): Flow<List<...>>`
- `observeArchived(): Flow<List<...>>`
- `observeByTag(tagId): Flow<List<...>>`
- `search(query): Flow<List<...>>` — title/excerpt/contentText/domain を `LIKE`。
- `getByNormalizedUrl(url): Article?` — 重複判定。
- `insert / update / delete`、`setRead`, `setArchived`, `setFolder`。

### TagDao
- `observeAll(): Flow<List<TagWithCount>>`、`getOrCreate(name): Long`、`delete`。

### FolderDao
- `observeAll(): Flow<List<FolderWithCount>>`、`insert / rename / delete`。

### ArticleTagDao
- `addTag(articleId, tagId)`、`removeTag(...)`、`tagsForArticle(articleId): Flow<List<Tag>>`。

## 4.4 関連オブジェクト（Room @Relation）

- `ArticleWithTags`: `Article` + `@Relation`（ArticleTag 経由の `List<Tag>`）。
- `TagWithCount` / `FolderWithCount`: 集計用（記事件数表示）。

## 4.5 マイグレーション方針

- スキーマ JSON をエクスポート（`room.schemaLocation`）し、バージョン管理に含める。
- 破壊的変更が必要な MVP 開発初期は `fallbackToDestructiveMigration` を一時的に許容、
  リリース後は `Migration` を明示実装。

## 4.6 将来拡張（参考）

- `Highlight`（articleId, 範囲, 色, メモ, createdAt）… 注釈機能で追加。
- 全文検索: `ArticleFts`（FTS4/5 仮想テーブル）を別途用意し検索を高速化。
- 同期用: `remoteId` / `syncState` / `deletedAt`（論理削除）をエンティティに追加。
