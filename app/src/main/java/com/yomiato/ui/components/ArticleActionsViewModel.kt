package com.yomiato.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.local.entity.ArticleEntity
import com.yomiato.data.local.entity.FolderEntity
import com.yomiato.data.local.entity.TagEntity
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.work.ArticleFetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 一覧画面群で共有する記事操作（既読/アーカイブ/タグ/フォルダ/削除/再取得）。
 * タグ・フォルダ一覧も提供し、操作シートやダイアログから利用する。
 */
@HiltViewModel
class ArticleActionsViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val scheduler: ArticleFetchScheduler,
) : ViewModel() {

    val allTags: StateFlow<List<TagEntity>> =
        repository.observeAllTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderEntity>> =
        repository.observeFolders()
            .map { list -> list.map { it.folder } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun tagsForArticle(articleId: Long): Flow<List<TagEntity>> =
        repository.observeTagsForArticle(articleId)

    fun toggleRead(article: ArticleEntity) = viewModelScope.launch {
        repository.setRead(article.id, !article.isRead)
    }

    fun toggleArchive(article: ArticleEntity) = viewModelScope.launch {
        repository.setArchived(article.id, !article.isArchived)
    }

    fun delete(articleId: Long) = viewModelScope.launch {
        repository.deleteArticle(articleId)
    }

    fun addTag(articleId: Long, name: String) = viewModelScope.launch {
        repository.addTagToArticle(articleId, name)
    }

    fun removeTag(articleId: Long, tagId: Long) = viewModelScope.launch {
        repository.removeTagFromArticle(articleId, tagId)
    }

    fun moveToFolder(articleId: Long, folderId: Long?) = viewModelScope.launch {
        repository.setFolder(articleId, folderId)
    }

    fun retry(articleId: Long) = viewModelScope.launch {
        repository.markPending(articleId)
        scheduler.enqueue(articleId)
    }
}
