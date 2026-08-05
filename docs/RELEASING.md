# Releasing

This project distributes signed APKs through GitHub Releases. The release signing key and passwords must remain outside the repository.

## Prepare

1. Start from a clean working tree and review the changelog.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Run the automated verification documented in [Testing](TESTING.md).
4. Complete the manual release matrix with the exact release candidate.

`versionCode` must increase for every APK distributed as an update. `versionName` should follow semantic versioning.

## Sign

In Android Studio, select **Build → Generate Signed Bundle / APK → APK**, choose the `app` module and release build type, then select the permanent Hifz Companion keystore. Keep the keystore in multiple encrypted backups and never commit it.

Verify the resulting APK's signer and copy its SHA-1 for the release Android OAuth client:

```powershell
apksigner verify --verbose --print-certs path\to\hifz-companion-v1.0.1.apk
```

Compute a download checksum:

```powershell
Get-FileHash path\to\hifz-companion-v1.0.1.apk -Algorithm SHA256
```

## Test the signed artifact

Install it over the previous signed version, then test a clean install. Run a real Drive backup, clear app data or use a second device, and restore using the same Google Account. Confirm the restored progress, history, and settings.

## Tag and publish

The tag must point to the exact source used for the APK:

```powershell
git tag -a v1.0.1 -m "Hifz Companion v1.0.1"
git push origin main
git push origin v1.0.1
```

Create a GitHub Release from that tag. Attach the signed APK, include the SHA-256 checksum and changelog summary, and explicitly mention any migration or backup compatibility notes. Download the published asset once and verify its checksum before sharing the release link.
