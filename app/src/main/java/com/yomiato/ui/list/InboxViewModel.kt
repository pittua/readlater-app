package com.yomiato.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.util.UrlUtils
import com.yomiato.data.work.ArticleFetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InboxFilter(val label: String) {
    INBOX("受信トレイ"),
    READ("既読"),
    ARCHIVED("アーカイブ"),
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val scheduler: ArticleFetchScheduler,
) : ViewModel() {

    private val _filter = MutableStateFlow(InboxFilter.INBOX)
    val filter: StateFlow<InboxFilter> = _filter.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val articles: StateFlow<List<ArticleWithTags>> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                InboxFilter.INBOX -> repository.observeInbox()
                InboxFilter.READ -> repository.observeInbox().map { list -> list.filter { it.article.isRead } }
                InboxFilter.ARCHIVED -> repository.observeArchived()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: InboxFilter) {
        _filter.value = filter
    }

    fun save(rawUrl: String) {
        val url = UrlUtils.extractFirstUrl(rawUrl) ?: rawUrl.trim()
        if (!UrlUtils.isValidHttpUrl(url)) {
            viewModelScope.launch { _messages.send("URL が見つかりません") }
            return
        }
        viewModelScope.launch {
            val result = repository.saveUrl(url)
            if (result.needsFetch) scheduler.enqueue(result.articleId)
            _messages.send(if (result.isNew) "保存しました" else "既存の記事を更新しました")
        }
    }
}
