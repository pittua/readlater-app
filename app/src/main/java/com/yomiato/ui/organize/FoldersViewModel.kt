package com.yomiato.ui.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.local.relation.FolderWithCount
import com.yomiato.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : ViewModel() {

    val folders: StateFlow<List<FolderWithCount>> =
        repository.observeFolders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) = viewModelScope.launch { repository.createFolder(name) }
    fun rename(id: Long, name: String) = viewModelScope.launch { repository.renameFolder(id, name) }
    fun delete(id: Long) = viewModelScope.launch { repository.deleteFolder(id) }
}
