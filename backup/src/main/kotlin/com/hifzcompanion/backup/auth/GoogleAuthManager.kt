package com.hifzcompanion.backup.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
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

sealed interface DriveAuthorizationResult {
    data class Granted(val session: GoogleSession) : DriveAuthorizationResult
    data class RequiresConsent(val pendingIntent: PendingIntent) : DriveAuthorizationResult
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val requestedScopes = listOf(Scope("https://www.googleapis.com/auth/drive.appdata"))

    suspend fun beginDriveAuthorization(activityContext: Context): DriveAuthorizationResult = withContext(Dispatchers.IO) {
        try {
            val result = Identity.getAuthorizationClient(activityContext)
                .authorize(AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build())
                .await()
            if (result.hasResolution()) {
                DriveAuthorizationResult.RequiresConsent(
                    result.pendingIntent ?: error("Google Drive consent could not be opened")
                )
            } else {
                DriveAuthorizationResult.Granted(result.toSession())
            }
        } catch (error: Exception) {
            throw error.asActionableOAuthError()
        }
    }

    fun completeDriveAuthorization(data: Intent): GoogleSession {
        return try {
            Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
                .toSession()
        } catch (error: Exception) {
            throw error.asActionableOAuthError()
        }
    }

    /** Silent refresh used by WorkManager after the user has already granted access. */
    suspend fun requestDriveAuthorization(activityContext: Context): String {
        return when (val authorization = beginDriveAuthorization(activityContext)) {
            is DriveAuthorizationResult.Granted -> authorization.session.accessToken
            is DriveAuthorizationResult.RequiresConsent -> error(
                "Google Drive access needs renewed consent. Open Hifz Companion Settings and run a manual backup."
            )
        }
    }

    private fun com.google.android.gms.auth.api.identity.AuthorizationResult.toSession(): GoogleSession {
        val token = accessToken ?: error("Google Drive did not return an access token")
        val account = toGoogleSignInAccount()
        return GoogleSession(account?.id ?: account?.email ?: "google-drive", token)
    }

    private fun Exception.asActionableOAuthError(): Exception {
        val details = message.orEmpty()
        return if ("28444" in details || "Developer console is not set up correctly" in details) {
            IllegalStateException(
                "Google OAuth does not match this Android build. Create an Android OAuth client for package " +
                    "ca.salmanfs.hifzcompanion with this build's SHA-1 signing fingerprint, enable the Drive API, " +
                    "and add the testing Google account to the OAuth consent screen.",
                this
            )
        } else this
    }
}
