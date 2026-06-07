package com.yomiato.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.ai.AiOutcome
import com.yomiato.data.local.relation.ArticleWithTags
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.settings.AppSettings
import com.yomiato.data.settings.SettingsRepository
import com.yomiato.data.work.ArticleFetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _suggestedTags = MutableStateFlow<List<String>>(emptyList())
    val suggestedTags: StateFlow<List<String>> = _suggestedTags.asStateFlow()

    val hasAiKey: Boolean get() = repository.hasAiKey()

    /** AI で要約＋タグ提案。要約は保存され、タグ候補は [suggestedTags] に提示される。 */
    fun summarize() {
        viewModelScope.launch {
            _busy.value = true
            when (val outcome = repository.summarizeAndTag(articleId)) {
                is AiOutcome.Success -> {
                    _suggestedTags.value = outcome.suggestedTags
                    _messages.send("要約しました")
                }
                AiOutcome.NoKey -> _messages.send("設定で Anthropic API キーを入力してください")
                is AiOutcome.Error -> _messages.send(outcome.message)
            }
            _busy.value = false
        }
    }

    /** 提案タグをワンタップで付与し、候補から消す。 */
    fun applySuggestedTag(name: String) {
        viewModelScope.launch {
            repository.addTagToArticle(articleId, name)
            _suggestedTags.value = _suggestedTags.value.filterNot { it.equals(name, ignoreCase = true) }
        }
    }

    fun dismissSuggestion(name: String) {
        _suggestedTags.value = _suggestedTags.value.filterNot { it == name }
    }

    /** オフライン保存（本文画像を埋め込み、通信なしで読めるようにする）。 */
    fun saveOffline() {
        viewModelScope.launch {
            _busy.value = true
            val ok = repository.saveOffline(articleId)
            _busy.value = false
            _messages.send(if (ok) "オフラインに保存しました" else "本文が無いためスナップショットで保存してください")
        }
    }

    /** スナップショット（MHTML）の保存先パス。UI の WebView が saveWebArchive で書き出す。 */
    fun snapshotPath(): String = repository.snapshotFileFor(articleId).absolutePath

    fun onSnapshotSaved() {
        viewModelScope.launch {
            repository.setSnapshotSaved(articleId, snapshotPath())
            _messages.send("ページをオフライン保存しました")
        }
    }

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
