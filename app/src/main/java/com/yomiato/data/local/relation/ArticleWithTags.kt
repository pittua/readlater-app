package com.yomiato.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.entity.ArticleTagCrossRef
import com.yomiato.data.local.entity.TagEntity

/** 記事 + 紐づくタグ一覧（多対多を @Relation で解決）。 */
data class ArticleWithTags(
    @Embedded val article: ArticleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArticleTagCrossRef::class,
            parentColumn = "articleId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
