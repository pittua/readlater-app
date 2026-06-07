package com.yomiato.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** 記事とタグの多対多を表す中間テーブル。 */
@Entity(
    tableName = "article_tags",
    primaryKeys = ["articleId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["articleId"]), Index(value = ["tagId"])],
)
data class ArticleTagCrossRef(
    val articleId: Long,
    val tagId: Long,
)
