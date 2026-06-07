package com.yomiato.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.settings.AppSettings
import com.yomiato.data.settings.SettingsRepository
import com.yomiato.data.work.ArticleFetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ArticleRepository,
    private val scheduler: ArticleFetchScheduler,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val articleId: Long = savedStateHandle.get<Long>("articleId") ?: -1L

    val article: StateFlow<ArticleWithTags?> =
        repository.observeArticle(articleId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val autoMarkReadFlow = settingsRepository.settings

    /** 詳細を開いたら（設定が有効なら）自動既読にする。一度だけ実行。 */
    fun onOpened() {
        viewModelScope.launch {
            val autoRead = autoMarkReadFlow.first().autoMarkRead
            val current = repository.observeArticle(articleId).first()
            if (autoRead && current != null && !current.article.isRead) {
                repository.setRead(articleId, true)
            }
        }
    }

    fun toggleArchive() = viewModelScope.launch {
        val current = article.value?.article ?: return@launch
        repository.setArchived(articleId, !current.isArchived)
    }

    fun retry() = viewModelScope.launch {
        repository.markPending(articleId)
        scheduler.enqueue(articleId)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteArticle(articleId)
        onDone()
    }
}
