# Privacy and data use

Hifz Companion is designed for private, personal use. It has no advertising SDK, behavioral tracking, or analytics upload service.

## Data stored on the device

The app stores memorized-page selections, revision schedules, review history, notes, preferences, and reminder settings in its private Android storage. Other apps cannot normally access this data.

Google OAuth access tokens are encrypted locally with a Tink key protected by Android Keystore. Local database and preference files are excluded from Android cloud backup and device transfer so unusable Keystore ciphertext is not silently restored.

## Optional Google Drive access

Google Drive backup is optional. The app requests only:

`https://www.googleapis.com/auth/drive.appdata`

This permits Hifz Companion to create and manage its own private application data. It does not permit the app to browse or modify ordinary files in the user's Drive.

The uploaded snapshot contains the app data described above as versioned JSON. Hifz Companion does not apply an additional end-to-end encryption layer; access is controlled by the user's Google Account and the private Drive AppData permission.

## Network communication

The app communicates with Google only for OAuth authorization and optional Drive backup/restore. Normal scheduling, statistics, review logging, settings, and notifications work offline.

## Data deletion

Uninstalling the app removes local application data. To remove the cloud backup, revoke Hifz Companion's account access and remove its hidden application data through Google account/Drive application management. Creating a new backup replaces the prior `hifz_backup.json` file in the app's Drive AppData area.

## Open-source notice

The source code is publicly available under the MIT License. Users can inspect how local data, OAuth authorization, token protection, and backup are implemented.
