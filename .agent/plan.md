# Project Plan

Hifz Companion: A personal hifz revision scheduler. 
Features:
- Local-first, Offline-first, No ads.
- Three queues: New, Long-term, Recovery.
- Intelligent scheduler with priority and load balancing.
- Quality levels: Strong, Good, Weak, Failed.
- Setup Wizard, Dashboard, Stats.
- Google Drive Backup.
- Modular Clean Architecture (app, core-ui, core-data, core-domain, scheduler, backup, settings, analytics, notifications).
- Scheduler module is pure Kotlin.
- Tech Stack: Kotlin, Compose, Material 3, Room, Hilt, Navigation, WorkManager, Google Drive API.
- Logo provided.
Please return the full Markdown content for the project brief.

## Project Brief

# Project Brief: Hifz Companion

Hifz Companion is a local-first, offline-first personal revision scheduler designed to help users maintain and improve their Hifz (memorization) progress. It focuses on a clean, ad-free experience with intelligent scheduling and cloud backup capabilities.

## Features

1.  **Smart Revision Dashboard**: A central hub to manage and visualize the three primary revision queues—**New**, **Long-term**, and **Recovery**—driven by an intelligent priority and load-balancing scheduler.
2.  **Revision Session Tracker**: A dedicated interface to log revision sessions where users assign quality levels (**Strong**, **Good**, **Weak**, or **Failed**) to update the scheduler's logic dynamically.
3.  **Onboarding & Goal Wizard**: A streamlined setup flow to initialize current memorization progress and define revision targets.
4.  **Google Drive Sync**: Secure, offline-first data persistence using Google Drive API for manual and automated backups, ensuring data remains private and portable.

## High-Level Technical Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with Material Design 3
*   **Navigation**: **Jetpack Navigation 3** (utilizing type-safe, state-driven destinations)
*   **Adaptive Strategy**: **Compose Material Adaptive** library (supporting Phone, Tablet, and Foldable layouts via Window Size Classes and Adaptive Scaffolding)
*   **Concurrency**: Kotlin Coroutines & Flow for asynchronous state management
*   **Persistence**: Room Database (critical for the local-first architecture)
*   **Dependency Injection**: Hilt
*   **Background Tasks**: WorkManager (for scheduling engine calculations and backup synchronization)
*   **Architecture**: Modular Clean Architecture (App, Core-UI, Core-Data, Scheduler, Backup, Settings)

## Implementation Steps
**Total Duration:** 24m 27s

### Task_1_Core_Foundation: Initialize the modular Clean Architecture project (App, Core, Scheduler, Backup). Implement the pure Kotlin Scheduler module (zero Android dependencies) with load balancing and queue logic (New, Long-term, Recovery). Setup Room database for offline-first persistence and Hilt for DI.
- **Status:** COMPLETED
- **Updates:** Initialized modular Clean Architecture project. Implemented Pure Kotlin Scheduler module with priority queues and load balancing. Setup Room database and Hilt. Verified with builds and unit tests.
- **Acceptance Criteria:**
  - Project structure created with required modules
  - Pure Kotlin Scheduler module implemented and tested
  - Room database and Hilt DI configured
  - Build pass
- **Duration:** 24m 27s

### Task_2_Onboarding_And_Dashboard: Implement the Onboarding & Goal Wizard using Jetpack Compose and Navigation 3. This includes Google Sign-In, Mushaf configuration, memorization progress initialization, and daily target setting. Develop the Smart Revision Dashboard to visualize the three primary revision queues.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Onboarding flow successfully initializes user data
  - Dashboard displays correct revision queues based on scheduler logic
  - Navigation between onboarding and dashboard is seamless
  - Adaptive UI support for different screen sizes
- **StartTime:** 2026-08-01 16:00:58 PDT

### Task_3_Session_Tracking_And_Sync: Build the Revision Session Tracker interface to log revision quality (Strong, Good, Weak, Failed) and update the scheduler. Integrate the Google Drive API for manual and automated data backups using WorkManager for background sync.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Revision sessions correctly update the scheduler and Room database
  - Google Drive backup and restore functionality working
  - WorkManager handles background sync tasks
  - Offline-first persistence verified

### Task_4_Final_Polish_And_Verification: Apply final UI/UX refinements, integrate the user-provided app logo, and perform comprehensive verification. Instruct critic_agent to verify stability and alignment with requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - App icon integrated
  - Final UI polish applied
  - All existing tests pass
  - App does not crash
  - Build pass
  - Critic_agent verifies stability and requirement alignment

## Codex Verified Status - 2026-08-02

The statuses above and in `analytics/.agent/plan.md` were compared with the 24-page Software Design Specification, source code, tests, and a debug build. The app has a working architectural foundation and an end-to-end onboarding -> dashboard -> review path, but it is not feature-complete. Prior claims that Google Drive backup, settings, analytics, and notifications are complete are not supported by the current implementation.

### Task_5_Logo_Adaptive_UI_And_Build_Repair
- **Status:** COMPLETED
- **Updates:**
  - Replaced the placeholder book icon with the supplied `ic_launcher_logo.png` asset.
  - Constrained onboarding and review content to a centered 720dp maximum width for tablets and foldables.
  - Prevented division by zero in the empty dashboard progress state.
  - Aligned the backup and settings modules to compileSdk 37, fixing a real AAR metadata build failure.
  - Verified `testDebugUnitTest` and `assembleDebug`: 8 tests passed, 0 failures; debug APK produced.

### Task_6_Production_Ready_Authentication_And_Backup
- **Status:** IMPLEMENTED_NEEDS_OAUTH_CONFIGURATION
- **Updates:** Removed hard-coded client IDs and dummy tokens; added build-time OAuth configuration, actionable auth states, encrypted token storage, atomic restore, Settings integration, and daily WorkManager backup. A real Google Cloud OAuth client is still required for live Drive verification.
- **Acceptance Criteria:**
  - Replace `YOUR_SERVER_CLIENT_ID` and `dummy_token` placeholders with configured Credential Manager and Drive authorization flows.
  - Surface sign-in/restore errors in onboarding UI.
  - Make Settings navigable and route manual backup/restore through a ViewModel.
  - Prove encrypted AppData backup and restore on a device or emulator.

### Task_7_Specification_Complete_Product_Surfaces
- **Status:** COMPLETED
- **Updates:** Added typed Settings, Statistics, Page Detail, and queue-list destinations; complete dashboard statistics and health labels; page history; morning/evening/weekly notification workers; and a non-empty analytics implementation.
- **Acceptance Criteria:**
  - Add Page Detail with review history.
  - Implement complete dashboard statistics: average quality, longest streak, missed reviews, backlog, average reviews/day, and the specified health-score inputs.
  - Implement morning, night, and weekly notifications.
  - Replace empty analytics and notifications modules with real implementations.

### Task_8_Scheduler_And_Settings_Compliance
- **Status:** COMPLETED
- **Updates:** Added persisted schedules/capacity/rotation/overload/review-order/recovery/theme/notification settings; explicit scheduler stages; configurable load balancing and recovery behavior; Room v1-to-v2 migration; and expanded scheduler tests.
- **Acceptance Criteria:**
  - Persist and honor configurable daily capacity, minimum pages, rotation days, overload percentage, schedules, review order, auto-advance, and theme.
  - Compute long-term load from `max(half juz, memorizedPages / rotationDays)` and use configured overload rather than fixed constructor values.
  - Implement explicit scheduler states and configurable recovery exit behavior from the specification.
  - Add tests for backlog spreading, prioritization under capacity, configurable schedules, state transitions, and date boundaries.

### Task_9_Onboarding_Accessibility_And_Device_Verification
- **Status:** IMPLEMENTED_NEEDS_DEVICE_MATRIX
- **Updates:** Added page-range, completed-surah, and individual-page setup using Madinah page boundaries; automatic target calculation; Compose test coverage; dark theme; and adaptive layouts. No emulator/device was connected, so phone/tablet/foldable runtime execution remains an external verification step.
- **Acceptance Criteria:**
  - Replace the 604-chip-only progress input with fast range/surah entry matching the specification examples.
  - Validate daily target ranges against real scheduler capacity.
  - Add Compose UI tests and run the complete flow on phone, tablet, and foldable profiles.
  - Verify app launch and crash-free behavior on an emulator/device; a successful APK build alone does not prove runtime stability.

## Feature Completion Verification - 2026-08-02

- `testDebugUnitTest`: 17 tests passed, 0 failures (13 scheduler, 2 onboarding mapping, 1 analytics, 1 app baseline).
- `compileDebugAndroidTestKotlin`: passed; Compose instrumentation tests compile.
- `lintDebug`: passed across the final source state.
- `assembleDebug`: passed; debug APK generated.
- `assembleRelease`: passed with lint-vital; unsigned release APK generated.
- No connected Android device was available, so instrumentation execution, live notification timing, and Google Drive backup/restore remain unverified at runtime.

## Requested Product Updates - 2026-08-03

### Task_10_Selection_Manual_Logging_And_Settings_Usability
- **Status:** COMPLETED
- **Updates:**
  - Replaced onboarding's numeric Surah range entry with a scrollable named-Surah selector and added Juz selection alongside page ranges and individual pages.
  - Removed the redundant Daily Target onboarding screen; the target remains automatically calculated.
  - Added Settings navigation for correcting current memorization while preserving review history for deselected pages.
  - Added an offline 604-page Madinah reference table so review and page-detail screens show Juz, Surah, and ayah ranges.
  - Added a dashboard flow for logging extra reviews or newly memorized pages by page, Surah, or Juz, including strength and optional notes. Manual completions update scheduler state and leave today's due queue.
  - Replaced free-text notification times with a 12-hour Material clock picker and added descriptions to scheduler controls.
  - Replaced the in-app and launcher logo with the supplied `hifz-companion-logo.png`.
  - Confirmed the user-level Google OAuth client property is configured and included at build time.
- **Verification:**
  - `testDebugUnitTest`: 22 tests passed, 0 failures, including stale next-page correction coverage.
  - `compileDebugAndroidTestKotlin`: passed.
  - `lintDebug`: passed.
  - `assembleDebug` and `assembleRelease`: passed.

### Task_11_Dashboard_Activity_And_Drive_Authorization
- **Status:** IMPLEMENTED_NEEDS_OAUTH_CONSOLE_VERIFICATION
- **Updates:**
  - Centered dashboard-card content and added dashboard Help and Today's Activity actions.
  - Added a same-day activity log with page references, review type, time, and editable revision strength.
  - Rating corrections now replay the page's complete review history through the scheduler so its queue, state, and next due date stay consistent.
  - Replaced numeric weekly-summary-day presentation with a Sunday-first weekday slider while preserving ISO weekday values in persistence.
  - Reworked Google Drive access around Google Identity Services `AuthorizationClient`, including user-consent resolution handling and actionable OAuth console errors. Drive AppData no longer depends on the Web client ID Gradle property.
- **Verification:**
  - `testDebugUnitTest` plus `:scheduler:test`: 23 tests passed, 0 failures, including the Strong-to-Weak activity correction regression.
  - `assembleDebug`: passed; debug APK generated.
  - Live Drive backup still requires the Google Cloud project to have an Android OAuth client registered for package `ca.salmanfs.hifzcompanion` and the installed build's SHA-1, Drive API enabled, and the account listed as a test user while the consent screen is in Testing.
