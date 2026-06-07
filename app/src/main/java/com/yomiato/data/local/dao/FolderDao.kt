package com.yomiato.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yomiato.data.local.entity.FolderEntity
import com.yomiato.data.local.relation.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT f.*, COUNT(a.id) AS articleCount
        FROM folders f
        LEFT JOIN articles a ON a.folderId = f.id
        GROUP BY f.id
        ORDER BY f.sortOrder, f.name
        """,
    )
    fun observeAllWithCount(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observeById(id: Long): Flow<FolderEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)
}
