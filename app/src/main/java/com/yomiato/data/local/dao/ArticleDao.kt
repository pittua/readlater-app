package com.yomiato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.relation.ArticleWithTags
import kotlinx.coroutines.flow.Flow

/**
 * 記事へのアクセス。一覧（受信トレイ / アーカイブ / フォルダ別 / タグ別）・検索・
 * 状態更新を提供する。一覧系はタグを同梱した [ArticleWithTags] を返す。
 */
@Dao
interface ArticleDao {

    // ---- 一覧（タグ同梱） ----

    @Transaction
    @Query("SELECT * FROM articles WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<ArticleWithTags>>

    @Transaction
    @Query("SELECT * FROM articles WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<ArticleWithTags>>

    @Transaction
    @Query("SELECT * FROM articles WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun observeByFolder(folderId: Long): Flow<List<ArticleWithTags>>

    @Transaction
    @Query(
        """
        SELECT a.* FROM articles a
        INNER JOIN article_tags at ON a.id = at.articleId
        WHERE at.tagId = :tagId
        ORDER BY a.createdAt DESC
        """,
    )
    fun observeByTag(tagId: Long): Flow<List<ArticleWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM articles
        WHERE title LIKE '%' || :query || '%'
           OR excerpt LIKE '%' || :query || '%'
           OR contentText LIKE '%' || :query || '%'
           OR domain LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """,
    )
    fun search(query: String): Flow<List<ArticleWithTags>>

    // ---- 単一記事 ----

    @Transaction
    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeWithTagsById(id: Long): Flow<ArticleWithTags?>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE normalizedUrl = :normalizedUrl LIMIT 1")
    suspend fun getByNormalizedUrl(normalizedUrl: String): ArticleEntity?

    // ---- 書き込み ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(article: ArticleEntity): Long

    @Update
    suspend fun update(article: ArticleEntity)

    @Query("UPDATE articles SET isRead = :read, updatedAt = :now WHERE id = :id")
    suspend fun setRead(id: Long, read: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET folderId = :folderId, updatedAt = :now WHERE id = :id")
    suspend fun setFolder(id: Long, folderId: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE articles SET createdAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun bumpToTop(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()
}
