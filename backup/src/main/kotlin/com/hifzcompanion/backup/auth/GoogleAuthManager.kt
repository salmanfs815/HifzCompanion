package com.hifzcompanion.backup.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleSession(
    val accountId: String,
    val accessToken: String
)

@Singleton
class GoogleAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context, serverClientId: String): GetCredentialResponse = withContext(Dispatchers.IO) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        credentialManager.getCredential(activityContext, request)
    }

    suspend fun requestDriveAuthorization(activityContext: Context): String = withContext(Dispatchers.IO) {
        val requestedScopes = listOf(Scope("https://www.googleapis.com/auth/drive.appdata"))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        val result = Identity.getAuthorizationClient(activityContext)
            .authorize(authorizationRequest)
            .await()
        result.accessToken ?: error("Google Drive authorization requires user consent")
    }

    suspend fun authorize(activityContext: Context, serverClientId: String): GoogleSession {
        require(serverClientId.isNotBlank()) {
            "Google OAuth is not configured. Set HIFZ_GOOGLE_SERVER_CLIENT_ID in Gradle properties."
        }
        val response = signIn(activityContext, serverClientId)
        val credential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
            .createFrom(response.credential.data)
        return GoogleSession(
            accountId = credential.id,
            accessToken = requestDriveAuthorization(activityContext)
        )
    }
}
