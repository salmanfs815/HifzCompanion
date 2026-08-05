# Testing

The suite is organized around fast policy tests, cross-layer workflow tests, Android UI tests, and a small manual release matrix.

## Automated JVM tests

Run all module unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Coverage includes:

- Scheduler transitions, queue priority, capacity, overload, custom intervals, recovery exit, inactive pages, and review ordering
- Onboarding page/Surah mapping
- Manual logging and activity-rating correction
- Health-label boundaries
- End-to-end offline journey: onboarding → daily queue → review → recovery/history
- End-to-end backup journey: snapshot → JSON encoding → upload → download → decoding → repository restore
- Malformed and missing backup handling without partial local restore

The backup end-to-end test uses in-memory Drive storage and separate source/destination repositories. This models a restore on a fresh installation without placing live Google credentials in CI.

## Android instrumentation tests

Start an emulator or connect a device, then run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Compose tests cover the optional Google Drive onboarding surface, actionable authorization errors, restore availability, and the offline skip path. Instrumentation also verifies the installed application ID.

## Static and build verification

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

The release APK produced by a normal Gradle build is unsigned unless a local signing configuration is provided. Use Android Studio's **Build → Generate Signed Bundle / APK** flow for distribution.

## Manual release matrix

Before publishing an APK, test the exact signed artifact:

- Clean install on the oldest supported version (Android 8/API 26)
- Current Android phone profile
- Dark theme and large font/display scaling
- Onboarding with Google Drive skipped
- Manual Google Drive backup using the release OAuth client
- Clear data or install on a second device, then restore from the same Google Account
- A malformed or interrupted backup fails without replacing local data
- Upgrade over the previous signed release without uninstalling
- Morning/evening notification permission and delivery
- Airplane-mode dashboard, review, history, statistics, and settings

Live Google OAuth and notification timing remain manual tests because they depend on an external account, consent UI, Play Services, and OS scheduling.
