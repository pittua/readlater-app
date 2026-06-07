package com.yomiato.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomiato.data.repository.ArticleRepository
import com.yomiato.data.settings.AppSettings
import com.yomiato.data.settings.SettingsRepository
import com.yomiato.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsRepository.setFontScale(scale) }
    fun setLineHeightScale(scale: Float) = viewModelScope.launch { settingsRepository.setLineHeightScale(scale) }
    fun setAutoMarkRead(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoMarkRead(enabled) }

    fun clearAllData() = viewModelScope.launch { articleRepository.deleteAllArticles() }
}
