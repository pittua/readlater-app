package com.yomiato.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.settings.AppSettings
import com.yomiato.data.settings.SettingsRepository
import com.yomiato.data.util.UrlUtils
import com.yomiato.data.work.ArticleFetchScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * アプリ全体スコープの ViewModel（Activity 保持）。
 * テーマ設定の供給と、共有シート（ACTION_SEND）からの保存を担う。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val scheduler: ArticleFetchScheduler,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts: Flow<String> = _toasts.receiveAsFlow()

    /** 共有テキストから URL を抽出して保存し、バックグラウンド取得を起動する。 */
    fun handleSharedText(text: String?) {
        val url = UrlUtils.extractFirstUrl(text)
        if (url == null || !UrlUtils.isValidHttpUrl(url)) {
            viewModelScope.launch { _toasts.send("URL が見つかりません") }
            return
        }
        viewModelScope.launch {
            val result = repository.saveUrl(url)
            if (result.needsFetch) scheduler.enqueue(result.articleId)
            _toasts.send(if (result.isNew) "記事を保存しました" else "保存済みの記事を更新しました")
        }
    }
}
