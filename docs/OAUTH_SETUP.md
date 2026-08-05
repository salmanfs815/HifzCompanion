# Google Drive OAuth Setup

Google sign-in is optional. The app remains fully usable offline when OAuth is not configured.

OAuth publishing is separate from Google Play or Android developer registration. Directly distributed APKs can use Google Drive authorization as long as their package/signing-certificate pair is registered here.

To enable Google Drive AppData backup and restore:

1. Create or select a Google Cloud project and enable the Google Drive API.
2. Configure the OAuth consent screen.
3. Create an **Android** OAuth client (not only a Web client) with:

   - Package name: `ca.salmanfs.hifzcompanion`
   - Current debug SHA-1: `FB:63:0B:7F:5C:37:42:06:C5:CD:7E:8F:DB:CB:A9:CC:31:42:49:25`

4. If the OAuth app is in Testing mode, add the device's Google account under **Audience → Test users**.
5. Wait a few minutes for Google Cloud configuration to propagate, uninstall the prior debug app if necessary, then install the newly built APK and connect again.

For release distribution, publish the OAuth audience to **In production**. The app requests only the non-sensitive `drive.appdata` scope. Leaving the audience in Testing limits access to listed test users and makes their authorizations expire after seven days.

The Drive-only flow now uses Google Identity Services `AuthorizationClient` directly and launches its consent resolution. A Web client ID and `HIFZ_GOOGLE_SERVER_CLIENT_ID` are not required for AppData backup. A separately signed release build must have its release certificate SHA-1 registered as another Android OAuth client.

Do not commit OAuth secrets or access tokens. Access tokens obtained at runtime are encrypted with a Tink key protected by Android Keystore before local persistence.

## Release certificate

Create a second Android OAuth client for the signed release APK. Use the same package name and the SHA-1 fingerprint of the release signing certificate. Keep the debug client for Android Studio builds. No Web OAuth client or client secret is used by this app.

If an APK is ever distributed through Google Play, register the Play app-signing certificate SHA-1 instead of the local upload certificate. Google Play re-signs delivered APKs.
