package com.hifzcompanion.backup.auth

import android.content.Context
import android.util.Base64
import com.hifzcompanion.backup.crypto.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cryptoManager: CryptoManager
) {
    private val preferences = context.getSharedPreferences("backup_session", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        val encrypted = cryptoManager.encrypt(token.toByteArray(Charsets.UTF_8))
        preferences.edit().putString(KEY_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    fun getAccessToken(): String? = runCatching {
        val value = preferences.getString(KEY_TOKEN, null) ?: return null
        String(cryptoManager.decrypt(Base64.decode(value, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    fun clear() = preferences.edit().clear().apply()

    private companion object { const val KEY_TOKEN = "encrypted_access_token" }
}
