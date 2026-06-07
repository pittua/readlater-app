package com.yomiato.di

import android.content.Context
import androidx.room.Room
import com.yomiato.data.local.AppDatabase
import com.yomiato.data.local.dao.ArticleDao
import com.yomiato.data.local.dao.FolderDao
import com.yomiato.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // 列追加では保存記事を失わないよう、正式な Migration を実装して適用する。
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
}
