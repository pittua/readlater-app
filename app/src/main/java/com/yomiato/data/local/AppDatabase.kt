package com.yomiato.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yomiato.data.local.dao.ArticleDao
import com.yomiato.data.local.dao.FolderDao
import com.yomiato.data.local.dao.TagDao
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.entity.ArticleTagCrossRef
import com.yomiato.data.local.entity.FolderEntity
import com.yomiato.data.local.entity.TagEntity

@Database(
    entities = [
        ArticleEntity::class,
        TagEntity::class,
        FolderEntity::class,
        ArticleTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val NAME = "yomiato.db"
    }
}
