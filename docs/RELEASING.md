# Releasing

This project distributes signed APKs through GitHub Releases and, once set up, app bundles through Google Play (see [Google Play](#google-play)). The release signing key and passwords must remain outside the repository.

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

## Google Play

### One-time setup

**Developer account.** Register a personal Play Console account (one-time fee, identity verification). New personal accounts must run a closed test with at least 12 opted-in testers for 14 consecutive days before production access is granted, so start that early.

**Play App Signing — decide before the first upload; it cannot be changed later.** Because signed APKs are already distributed on GitHub, choose **Use existing app signing key** and upload the permanent Hifz Companion keystore with Play Console's PEPK tool. Then create a separate *upload key* for signing bundles sent to Play, and back it up the same way as the app signing key.

- This keeps GitHub and Play installs signed with the same certificate, so each can update the other and the existing release Android OAuth client keeps working.
- If Play generates a new app signing key instead, GitHub and Play installs can never update each other, and the Play app signing certificate's SHA-1 (Play Console → Test and release → App integrity) must be registered as an additional Android OAuth client. See [Google Drive OAuth Setup](OAUTH_SETUP.md).

**OAuth consent screen.** Confirm the audience is **In production** and that the consent screen links to the hosted privacy policy and homepage.

**Store listing assets.** Prepare a 512×512 icon, a 1024×500 feature graphic, at least two phone screenshots, a short description (≤ 80 characters), and a full description (≤ 4000 characters). The [User guide](USER_GUIDE.md) is a good source of copy.

**App content forms.**

- *Privacy policy:* host [Privacy and data use](PRIVACY.md) at a public URL (for example GitHub Pages) and enter it.
- *Data safety:* declare app activity / user-generated content (memorization progress, review history, notes) as collected, optional, encrypted in transit, not shared, and deletable by the user — this is the optional Drive backup. No ads, analytics, location, or contacts.
- *App access:* all functionality is available without signing in; Google Drive backup is optional.
- *Target audience:* including users under 13 brings the app under the Families policy. Choose 13+ unless children are a primary audience.
- *Content rating:* complete the questionnaire (expected: Everyone).
- *Account deletion:* not applicable — the app has no accounts of its own; Google sign-in only authorizes Drive AppData access.

### Each release

1. Complete [Prepare](#prepare). GitHub APKs and Play bundles share one `versionCode` sequence, so never reuse or lower a value.
2. In Android Studio, select **Build → Generate Signed Bundle / APK → Android App Bundle**, choose the release build type, and sign with the **upload key**.
3. Upload the `.aab` to the **Internal testing** track and install it from Play on a real device. Run a Drive backup and restore there — this verifies the Play-delivered signature against the registered OAuth clients.
4. Review the pre-launch report for crashes and accessibility warnings.
5. Promote the release to **Closed testing** (while the 14-day requirement is outstanding) or **Production**. Paste the changelog summary into the release notes.
6. Start production with a staged rollout (for example 20%), watch Android vitals for crashes and ANRs, then increase to 100%.
7. Tag the exact source as described in [Tag and publish](#tag-and-publish), and publish the matching GitHub APK if distributing both.
