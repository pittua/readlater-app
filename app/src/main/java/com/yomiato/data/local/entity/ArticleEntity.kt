package com.yomiato.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yomiato.data.local.ExtractionStatus

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["normalizedUrl"], unique = true),
        Index(value = ["folderId"]),
        Index(value = ["isArchived"]),
        Index(value = ["createdAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val normalizedUrl: String,
    val title: String? = null,
    val siteName: String? = null,
    val domain: String? = null,
    val thumbnailUrl: String? = null,
    val excerpt: String? = null,
    val contentHtml: String? = null,
    val contentText: String? = null,
    val estimatedReadMinutes: Int? = null,
    val folderId: Long? = null,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val extractionStatus: ExtractionStatus = ExtractionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
