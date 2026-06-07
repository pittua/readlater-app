package com.yomiato.data.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API キーなどの機微情報を暗号化して保存する（EncryptedSharedPreferences）。
 * プレーンな DataStore には保存しない。
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "yomiato_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var anthropicApiKey: String?
        get() = prefs.getString(KEY_ANTHROPIC, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ANTHROPIC) else putString(KEY_ANTHROPIC, value.trim())
            }.apply()
        }

    fun hasAnthropicKey(): Boolean = !anthropicApiKey.isNullOrBlank()

    private companion object {
        const val KEY_ANTHROPIC = "anthropic_api_key"
    }
}
