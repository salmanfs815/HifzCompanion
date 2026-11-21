# Hifz Companion user guide

## Install or update

Download the signed APK from the repository's [latest release](https://github.com/salmanfs815/HifzCompanion/releases/latest). Android may ask you to permit installs from the browser or file manager opening the APK.

For an update, install the new APK over the current version. Keeping the existing installation preserves local data. If Android reports that the package conflicts with an existing app, do not uninstall until your data has been backed up; the conflicting APK may have been signed with a different certificate.

## First-time setup

Google Drive is optional. You may skip it and use every scheduling and tracking feature offline.

Confirm the Mushaf type (currently only the 15-line, 604-page Madinah Mushaf is supported), then choose the memorization order and the pages currently memorized. Pages can be selected efficiently by page range, Surah, or Juz. The app creates a long-term revision rotation for memorized pages and suggests the next page for memorization.

## Daily revision

The dashboard groups work into three queues:

- **New:** recently memorized pages being stabilized.
- **Long-term:** stable pages rotating through regular revision.
- **Recovery:** weak pages receiving short-interval attention.

Use these ratings consistently:

| Rating | Suggested meaning | Scheduling effect |
|---|---|---|
| Strong | Fluent and confident | Advances faster or extends the interval |
| Good | Correct with minor hesitation | Advances normally |
| Weak | Noticeable prompting or repeated hesitation | Moves to recovery |
| Failed | Could not complete reliably | Enters the failed-page recovery schedule |

Today's Activity lets you correct an accidental rating. Hifz Companion replays the page's review history so its current schedule remains consistent.

## Manual reviews

Use manual logging for an extra review or a page memorized outside today's queue. Select pages by page, Surah, or Juz, choose the result, and optionally record notes.

## Google Drive backup

Backups use the private Google Drive AppData area and are not visible among ordinary Drive documents.

1. Open **Settings → Backup & restore**.
2. Select **Back up to Google Drive** and approve Google access if requested.
3. Wait for the “Backup complete” message.

To restore after reinstalling or changing phones:

1. Install Hifz Companion and select **Restore from Cloud** during onboarding, or open Settings.
2. Select the same Google Account used for backup.
3. Wait for the restore confirmation before continuing.

The snapshot is stored in the app's private Google Drive AppData area. It does not appear among ordinary Drive files and can only be accessed through an authorized Hifz Companion installation for that account.

## Notifications

Enable reminders in Settings and select morning/evening times plus a weekly summary day. Android 13 and newer also require notification permission. Battery optimization or manufacturer background restrictions can delay WorkManager notifications.

## Troubleshooting

- **OAuth configuration mismatch:** The installed APK's package name and signing SHA-1 must be registered in the app's Google Cloud project.
- **Backup not found:** Confirm that the same Google Account created the backup.
- **App will not update:** Confirm the new APK is newer and signed by the same release key.
- **Reminders are late:** Remove restrictive battery settings for Hifz Companion and check notification permission.
