# Google Drive OAuth Setup

Google sign-in is optional. The app remains fully usable offline when OAuth is not configured.

To enable encrypted Google Drive AppData backup and restore:

1. Create or select a Google Cloud project and enable the Google Drive API.
2. Configure the OAuth consent screen.
3. Create an Android OAuth client for package `ca.salmanfs.hifzcompanion` using the signing certificate SHA-1.
4. Create a Web application OAuth client for Credential Manager and copy its client ID.
5. Add the client ID to the user-level Gradle properties file (`%USERPROFILE%\.gradle\gradle.properties`):

   ```properties
   HIFZ_GOOGLE_SERVER_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
   ```

Do not commit OAuth secrets or access tokens. Access tokens obtained at runtime are encrypted with a Tink key protected by Android Keystore before local persistence.
