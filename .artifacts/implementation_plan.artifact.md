# Dashboard and Revision Session Tracker Implementation Plan

Complete the Dashboard polish and implement the Revision Session Tracker to allow users to review and rate their Quran memorization progress.

## User Review Required

> [!IMPORTANT]
> The transition logic for "Strong" and "Good" will be implemented as follows:
> - **Strong**: Move 2 steps forward in the interval sequence (or double the interval for long-term).
> - **Good**: Move 1 step forward in the interval sequence (keep the standard increment).
> - **Weak**: Move to Recovery Weak queue (step 0).
> - **Failed**: Move to New memorization queue (step 0).

## Proposed Changes

### [Component] Scheduler

#### [MODIFY] [ReviewResult.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/scheduler/src/main/kotlin/com/hifzcompanion/scheduler/models/ReviewResult.kt)
Update enum to `STRONG`, `GOOD`, `WEAK`, `FAILED`.

#### [MODIFY] [HifzScheduler.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/scheduler/src/main/kotlin/com/hifzcompanion/scheduler/HifzScheduler.kt)
- Update `scheduleNext` to handle the new rating levels and transition logic.
- Fix `LONG_TERM` scheduling bug (ensure `nextReviewDate` is updated).

---

### [Component] Data & Domain Layer

#### [MODIFY] [HifzRepository.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-domain/src/main/kotlin/com/hifzcompanion/core/domain/repository/HifzRepository.kt)
Add `fun getTodayReviewCount(): Flow<Int>`.

#### [MODIFY] [HifzRepositoryImpl.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-data/src/main/kotlin/com/hifzcompanion/core/data/repository/HifzRepositoryImpl.kt)
Implement `getTodayReviewCount` using `HifzDao`.

#### [MODIFY] [HifzDao.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-data/src/main/kotlin/com/hifzcompanion/core/data/dao/HifzDao.kt)
Add query to count review events for a specific date.

#### [MODIFY] [GetDashboardStatsUseCase.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/core-domain/src/main/kotlin/com/hifzcompanion/core/domain/usecase/GetDashboardStatsUseCase.kt)
Integrate `getTodayReviewCount` into the stats.

---

### [Component] UI Layer (Dashboard)

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/app/src/main/java/ca/salmanfs/hifzcompanion/ui/dashboard/DashboardScreen.kt)
- Improve the progress bar display.
- Wire the "Start Today's Session" button to navigate to the Review screen.

---

### [Component] UI Layer (Review Session)

#### [NEW] [ReviewSessionScreen.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/app/src/main/java/ca/salmanfs/hifzcompanion/ui/review/ReviewSessionScreen.kt)
- Pager or List interface to review pages.
- Rating buttons with M3 colors and icons.
- Completion state.

#### [NEW] [ReviewSessionViewModel.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/app/src/main/java/ca/salmanfs/hifzcompanion/ui/review/ReviewSessionViewModel.kt)
- Manage the list of pages to review today using `scheduler.getDailyTasks`.
- Handle rating actions: update progress in DB and record events.

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/salma/AndroidStudioProjects/HifzCompanion/app/src/main/java/ca/salmanfs/hifzcompanion/navigation/AppNavigation.kt)
Add the Review Session route using Navigation 3.

## Verification Plan

### Automated Tests
- Run existing scheduler tests: `./gradlew :scheduler:test`.
- Add unit tests for `HifzScheduler` new transition logic.
- Run app build: `./gradlew :app:assembleDebug`.

### Manual Verification
- Verify Dashboard correctly shows progress (e.g., 0/N initially).
- Start session, rate a page, and verify Dashboard updates (1/N).
- Check if rated pages disappear from the "Today's Review" count if they are no longer due (or stay in progress).
