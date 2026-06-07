package com.yomiato.data.local.relation

import androidx.room.Embedded
import com.yomiato.data.local.entity.TagEntity

/** タグ + 紐づく記事件数（タグ一覧表示用）。 */
data class TagWithCount(
    @Embedded val tag: TagEntity,
    val articleCount: Int,
)
