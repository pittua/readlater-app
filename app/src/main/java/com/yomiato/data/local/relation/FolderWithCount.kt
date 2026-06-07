package com.yomiato.data.local.relation

import androidx.room.Embedded
import com.yomiato.data.local.entity.FolderEntity

/** フォルダ + 中の記事件数（フォルダ一覧表示用）。 */
data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val articleCount: Int,
)
