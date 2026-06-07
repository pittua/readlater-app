package com.yomiato.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val NAME = "yomiato.db"

        /** v1→v2: AI 要約・オフライン保存用のカラムを追加（既存データは保持）。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN summary TEXT")
                db.execSQL("ALTER TABLE articles ADD COLUMN summaryModel TEXT")
                db.execSQL("ALTER TABLE articles ADD COLUMN summarizedAt INTEGER")
                db.execSQL("ALTER TABLE articles ADD COLUMN offlineContentHtml TEXT")
                db.execSQL("ALTER TABLE articles ADD COLUMN snapshotPath TEXT")
            }
        }
    }
}
