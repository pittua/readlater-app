package com.yomiato.data.repository

import com.yomiato.data.local.ExtractionStatus
import com.yomiato.data.local.dao.ArticleDao
import com.yomiato.data.local.dao.FolderDao
import com.yomiato.data.local.dao.TagDao
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.entity.ArticleTagCrossRef
import com.yomiato.data.local.entity.FolderEntity
import com.yomiato.data.local.entity.TagEntity
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.local.relation.FolderWithCount
import com.yomiato.data.local.relation.TagWithCount
import com.yomiato.data.remote.ArticleFetcher
import com.yomiato.data.util.UrlUtils
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 記事・タグ・フォルダのデータ操作を集約する単一の窓口。
 * UI(ViewModel) と Worker はここだけを介して DB / ネットワークにアクセスする。
 */
@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val tagDao: TagDao,
    private val folderDao: FolderDao,
    private val fetcher: ArticleFetcher,
) {
    // ---- 一覧 ----
    fun observeInbox(): Flow<List<ArticleWithTags>> = articleDao.observeInbox()
    fun observeArchived(): Flow<List<ArticleWithTags>> = articleDao.observeArchived()
    fun observeByFolder(folderId: Long): Flow<List<ArticleWithTags>> = articleDao.observeByFolder(folderId)
    fun observeByTag(tagId: Long): Flow<List<ArticleWithTags>> = articleDao.observeByTag(tagId)
    fun search(query: String): Flow<List<ArticleWithTags>> = articleDao.search(query)

    // ---- 単一記事 ----
    fun observeArticle(id: Long): Flow<ArticleWithTags?> = articleDao.observeWithTagsById(id)

    // ---- 保存 ----

    /**
     * URL を受け取り PENDING 状態で保存する。重複（正規化後一致）の場合は新規作成せず
     * 既存記事を先頭へ繰り上げる。戻り値は対象記事 ID（取得を起動すべきか [needsFetch] で判定）。
     */
    suspend fun saveUrl(rawUrl: String): SaveResult {
        val url = rawUrl.trim()
        val normalized = UrlUtils.normalize(url)

        val existing = articleDao.getByNormalizedUrl(normalized)
        if (existing != null) {
            articleDao.bumpToTop(existing.id)
            // 取得が終わっていない/失敗していれば再取得対象にする
            val needsFetch = existing.extractionStatus != ExtractionStatus.SUCCESS
            return SaveResult(existing.id, isNew = false, needsFetch = needsFetch)
        }

        val now = System.currentTimeMillis()
        val id = articleDao.insert(
            ArticleEntity(
                url = url,
                normalizedUrl = normalized,
                domain = UrlUtils.host(url),
                extractionStatus = ExtractionStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return SaveResult(id, isNew = true, needsFetch = true)
    }

    /**
     * バックグラウンド取得本体。HTML 取得・抽出を行い記事を SUCCESS/FAILED で更新する。
     * Worker から呼ばれる。例外はここで捕捉し FAILED として記録する。
     */
    suspend fun fetchAndUpdate(articleId: Long): Boolean {
        val article = articleDao.getById(articleId) ?: return false
        return try {
            val result = fetcher.fetch(article.url)
            articleDao.update(
                article.copy(
                    title = result.title ?: article.title,
                    siteName = result.siteName ?: article.siteName,
                    domain = result.domain ?: article.domain,
                    thumbnailUrl = result.thumbnailUrl ?: article.thumbnailUrl,
                    excerpt = result.excerpt ?: article.excerpt,
                    contentHtml = result.contentHtml,
                    contentText = result.contentText,
                    estimatedReadMinutes = result.estimatedReadMinutes,
                    extractionStatus = ExtractionStatus.SUCCESS,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            true
        } catch (e: Exception) {
            articleDao.update(
                article.copy(
                    extractionStatus = ExtractionStatus.FAILED,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            false
        }
    }

    /** 再取得のため PENDING に戻す。Worker 起動は呼び出し側で行う。 */
    suspend fun markPending(articleId: Long) {
        val article = articleDao.getById(articleId) ?: return
        articleDao.update(article.copy(extractionStatus = ExtractionStatus.PENDING, updatedAt = System.currentTimeMillis()))
    }

    // ---- 記事の状態 ----
    suspend fun setRead(id: Long, read: Boolean) = articleDao.setRead(id, read)
    suspend fun setArchived(id: Long, archived: Boolean) = articleDao.setArchived(id, archived)
    suspend fun setFavorite(id: Long, favorite: Boolean) = articleDao.setFavorite(id, favorite)
    suspend fun setFolder(id: Long, folderId: Long?) = articleDao.setFolder(id, folderId)
    suspend fun deleteArticle(id: Long) = articleDao.delete(id)
    suspend fun deleteAllArticles() = articleDao.deleteAll()

    // ---- タグ ----
    fun observeTags(): Flow<List<TagWithCount>> = tagDao.observeAllWithCount()
    fun observeAllTags(): Flow<List<TagEntity>> = tagDao.observeAll()
    fun observeTag(id: Long): Flow<TagEntity?> = tagDao.observeById(id)
    fun observeTagsForArticle(articleId: Long): Flow<List<TagEntity>> = tagDao.tagsForArticle(articleId)

    /** タグ名を正規化して取得 or 作成し、記事に付与する。 */
    suspend fun addTagToArticle(articleId: Long, rawName: String) {
        val name = normalizeTagName(rawName)
        if (name.isEmpty()) return
        val tagId = getOrCreateTag(name)
        tagDao.addTagToArticle(ArticleTagCrossRef(articleId, tagId))
    }

    suspend fun removeTagFromArticle(articleId: Long, tagId: Long) =
        tagDao.removeTagFromArticle(articleId, tagId)

    suspend fun deleteTag(id: Long) = tagDao.delete(id)

    private suspend fun getOrCreateTag(name: String): Long {
        tagDao.getByName(name)?.let { return it.id }
        val id = tagDao.insert(TagEntity(name = name))
        // INSERT IGNORE で 0 件のとき（競合）は再取得
        return if (id > 0) id else tagDao.getByName(name)?.id ?: id
    }

    private fun normalizeTagName(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT).let { lower ->
            // 表示は入力どおりにしたいので大文字小文字統一はキー比較のみに留めたいが、
            // MVP では一意性確保のため小文字保存とする。
            lower
        }

    // ---- フォルダ ----
    fun observeFolders(): Flow<List<FolderWithCount>> = folderDao.observeAllWithCount()
    fun observeFolder(id: Long): Flow<FolderEntity?> = folderDao.observeById(id)

    suspend fun createFolder(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1
        return folderDao.insert(FolderEntity(name = trimmed))
    }

    suspend fun renameFolder(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) folderDao.rename(id, trimmed)
    }

    suspend fun deleteFolder(id: Long) = folderDao.delete(id)
}

data class SaveResult(
    val articleId: Long,
    val isNew: Boolean,
    val needsFetch: Boolean,
)
