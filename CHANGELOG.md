# Changelog

All notable user-facing changes are documented here.

## [1.1.0] - 2026-11-22

### Added

- An intro guide in onboarding and on the dashboard.
- Priority and page-number sorting for today's review.
- Tips on the Statistics screen explaining the health score.

### Changed

- The health score is now shown as proportions.
- Page boundaries now follow surah boundaries; in the manual log, a page shared by two surahs is credited to the surah with most of its lines.
- A surah that shares a page can now be unticked.
- Today's review scrolls back to the top after reordering.
- The minimum pages setting is now enforced.
- Mushaf selection is limited to the Madinah Mushaf.

## [1.0.1] - 2026-08-03

### Added

- Automated cross-installation backup and primary offline-journey tests.
- Portfolio README plus user, architecture, privacy, OAuth, and testing documentation.

### Changed

- Simplified Google Drive backup to a versioned JSON snapshot in the private AppData area.
- Removed unused starter resources and resolved project-owned lint warnings.
- Recreate scheduled review reminders after a device restart or app update.
- Use a recognizable book icon for review notifications and open the app when they are tapped.
- Keep the dashboard Recovery count synchronized with Weak and Failed pages, including ratings from today's session.

## [1.0.0] - 2026-08-03

- Initial release.
- Offline revision scheduling, review tracking, statistics, notifications, settings, and optional Google Drive backup.
