package com.yomiato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yomiato.data.local.entity.ArticleTagCrossRef
import com.yomiato.data.local.entity.TagEntity
import com.yomiato.data.local.relation.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.*, COUNT(at.articleId) AS articleCount
        FROM tags t
        LEFT JOIN article_tags at ON t.id = at.tagId
        GROUP BY t.id
        ORDER BY t.name
        """,
    )
    fun observeAllWithCount(): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags WHERE id = :id")
    fun observeById(id: Long): Flow<TagEntity?>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN article_tags at ON t.id = at.tagId
        WHERE at.articleId = :articleId
        ORDER BY t.name
        """,
    )
    fun tagsForArticle(articleId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToArticle(crossRef: ArticleTagCrossRef)

    @Query("DELETE FROM article_tags WHERE articleId = :articleId AND tagId = :tagId")
    suspend fun removeTagFromArticle(articleId: Long, tagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)
}
