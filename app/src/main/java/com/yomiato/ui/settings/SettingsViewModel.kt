package com.yomiato.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.ai.SecureKeyStore
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.settings.AppSettings
import com.yomiato.data.settings.SettingsRepository
import com.yomiato.data.settings.SummaryEngine
import com.yomiato.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val articleRepository: ArticleRepository,
    private val secureKeyStore: SecureKeyStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _hasApiKey = MutableStateFlow(secureKeyStore.hasAnthropicKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun setApiKey(key: String) {
        secureKeyStore.anthropicApiKey = key
        _hasApiKey.value = secureKeyStore.hasAnthropicKey()
    }

    fun clearApiKey() {
        secureKeyStore.anthropicApiKey = null
        _hasApiKey.value = false
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsRepository.setFontScale(scale) }
    fun setLineHeightScale(scale: Float) = viewModelScope.launch { settingsRepository.setLineHeightScale(scale) }
    fun setAutoMarkRead(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoMarkRead(enabled) }

    fun setSummaryEngine(engine: SummaryEngine) = viewModelScope.launch { settingsRepository.setSummaryEngine(engine) }

    fun clearAllData() = viewModelScope.launch { articleRepository.deleteAllArticles() }
}
