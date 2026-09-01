package com.hifzcompanion.backup.auth

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.hifzcompanion.backup.crypto.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cryptoManager: CryptoManager
) {
    private val preferences = context.getSharedPreferences("backup_session", Context.MODE_PRIVATE)
    private val _lastSuccessfulBackup = MutableStateFlow(
        preferences.getLong(KEY_LAST_SUCCESSFUL_BACKUP, -1L).takeIf { it >= 0L }
    )
    val lastSuccessfulBackup: StateFlow<Long?> = _lastSuccessfulBackup

    fun saveAccessToken(token: String) {
        val encrypted = cryptoManager.encrypt(token.toByteArray(Charsets.UTF_8))
        preferences.edit { putString(KEY_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP)) }
    }

    fun getAccessToken(): String? = runCatching {
        val value = preferences.getString(KEY_TOKEN, null) ?: return null
        String(cryptoManager.decrypt(Base64.decode(value, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    fun recordSuccessfulBackup(timestamp: Long = System.currentTimeMillis()) {
        preferences.edit { putLong(KEY_LAST_SUCCESSFUL_BACKUP, timestamp) }
        _lastSuccessfulBackup.value = timestamp
    }

    fun clear() {
        preferences.edit { clear() }
        _lastSuccessfulBackup.value = null
    }

    private companion object {
        const val KEY_TOKEN = "encrypted_access_token"
        const val KEY_LAST_SUCCESSFUL_BACKUP = "last_successful_backup"
    }
}
