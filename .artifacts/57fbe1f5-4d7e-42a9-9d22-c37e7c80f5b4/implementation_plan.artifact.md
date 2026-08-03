# Implementation Plan - Google Drive Sync and Backups

Implement a robust backup and restore system for Hifz Companion using Google Drive AppData storage.

## User Review Required

> [!IMPORTANT]
> The user will need to sign in with their Google account to enable cloud backups. This will be integrated into the Onboarding/Setup Wizard and the Settings screen.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/gradle/libs.versions.toml)
- Add dependencies for Google Drive API, Credential Manager, WorkManager, Tink (for encryption), and Kotlinx Serialization.

#### [MODIFY] [backup/build.gradle.kts](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/build.gradle.kts)
- Configure the module with necessary dependencies and plugins (Hilt, Serialization).

### Core Data Enhancements

#### [MODIFY] [HifzDao.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-data/src/main/kotlin/com/hifzcompanion/core/data/dao/HifzDao.kt)
- Add `suspend` methods to fetch all records for all tables (for backup).
- Add `suspend` methods to clear and insert all records (for restore).

#### [NEW] [BackupRepository.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-data/src/main/kotlin/com/hifzcompanion/core/data/repository/BackupRepository.kt)
- Interface for creating and restoring snapshots.

#### [NEW] [BackupRepositoryImpl.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-data/src/main/kotlin/com/hifzcompanion/core/data/repository/BackupRepositoryImpl.kt)
- Implementation that aggregates Room data and DataStore settings into a snapshot.

### Backup Module Implementation

#### [NEW] [BackupSnapshot.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/src/main/kotlin/com/hifzcompanion/backup/model/BackupSnapshot.kt)
- Data class representing the encrypted JSON snapshot.

#### [NEW] [CryptoManager.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/src/main/kotlin/com/hifzcompanion/backup/crypto/CryptoManager.kt)
- Handles encryption and decryption of snapshots using `androidx.security.crypto`.

#### [NEW] [GoogleDriveManager.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/src/main/kotlin/com/hifzcompanion/backup/drive/GoogleDriveManager.kt)
- Handles Google Auth and Drive API interactions (AppData folder).

#### [NEW] [BackupWorker.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/src/main/kotlin/com/hifzcompanion/backup/worker/BackupWorker.kt)
- WorkManager implementation for automated daily backups.

#### [NEW] [BackupManager.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/backup/src/main/kotlin/com/hifzcompanion/backup/BackupManager.kt)
- Orchestrator that coordinates between `BackupRepository`, `CryptoManager`, and `GoogleDriveManager`.

### UI Integration

#### [NEW] [SettingsScreen.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/settings/src/main/kotlin/com/hifzcompanion/settings/ui/SettingsScreen.kt)
- UI for manual backup/restore and Google account management.

#### [MODIFY] [OnboardingScreen.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/app/src/main/kotlin/ca/salmanfs/hifzcompanion/ui/onboarding/OnboardingScreen.kt)
- Add "Restore from Cloud" button during setup.

## Verification Plan

### Automated Tests
- Unit tests for `CryptoManager` to verify encryption/decryption.
- Unit tests for `BackupRepositoryImpl` to verify snapshot creation/restoration.

### Manual Verification
- Trigger manual backup from Settings and verify file exists in Google Drive AppData (via logs).
- Uninstall and reinstall app, then trigger restore during Onboarding and verify data is back.
- Wait for daily backup trigger (or force trigger via ADB) and verify success.
