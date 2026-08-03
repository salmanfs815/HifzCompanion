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

