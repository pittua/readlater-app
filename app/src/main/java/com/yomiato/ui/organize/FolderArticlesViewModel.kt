package com.yomiato.ui.organize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FolderArticlesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ArticleRepository,
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: -1L

    val title: StateFlow<String> =
        repository.observeFolder(folderId)
            .map { it?.name ?: "フォルダ" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "フォルダ")

    val articles: StateFlow<List<ArticleWithTags>> =
        repository.observeByFolder(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
