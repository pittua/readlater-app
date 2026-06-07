package com.yomiato.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore(Preferences) による設定の永続化。 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            readerFontScale = prefs[KEY_FONT_SCALE] ?: 1.0f,
            readerLineHeightScale = prefs[KEY_LINE_HEIGHT] ?: 1.0f,
            autoMarkRead = prefs[KEY_AUTO_MARK_READ] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) =
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }

    suspend fun setFontScale(scale: Float) =
        dataStore.edit { it[KEY_FONT_SCALE] = scale }

    suspend fun setLineHeightScale(scale: Float) =
        dataStore.edit { it[KEY_LINE_HEIGHT] = scale }

    suspend fun setAutoMarkRead(enabled: Boolean) =
        dataStore.edit { it[KEY_AUTO_MARK_READ] = enabled }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val KEY_LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val KEY_AUTO_MARK_READ = booleanPreferencesKey("auto_mark_read")
    }
}
