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
class TagArticlesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ArticleRepository,
) : ViewModel() {

    private val tagId: Long = savedStateHandle.get<Long>("tagId") ?: -1L

    val title: StateFlow<String> =
        repository.observeTag(tagId)
            .map { it?.name?.let { name -> "#$name" } ?: "タグ" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "タグ")

    val articles: StateFlow<List<ArticleWithTags>> =
        repository.observeByTag(tagId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
