# Project Initialization Walkthrough - Hifz Companion

The Hifz Companion project has been initialized with a modular Clean Architecture structure.

## Modules Created
- `:app`: Android application module, setup with Hilt and Navigation 3.
- `:scheduler`: Pure Kotlin module containing the core memorization scheduling logic.
- `:core-domain`: Android library for domain entities and repository interfaces.
- `:core-data`: Android library implementing the data layer with Room and Hilt.
- `:core-ui`: Android library for shared UI components.
- `:backup`, `:settings`, `:analytics`, `:notifications`: Feature modules (placeholders).

## Key Implementations

### Scheduler Logic (`:scheduler`)
Implemented `HifzScheduler` which manages three review queues:
1. **New Memorization**: 12-step interval schedule.
2. **Long-term Rotation**: Default 14-day rotation.
3. **Recovery Queue**: Handles "Weak" and "Failed" items with specific intervals.
- **Priority**: Recovery > New > Long-term.
- **Load Balancing**: Limits daily tasks to 150% of capacity.
- **Long-term Algorithm**: `pagesPerDay = max(10, memorizedPages / rotationDays)`.

### Data Layer (`:core-data`)
- **Room Database**: `AppDatabase` with entities for `QuranPage`, `UserPage`, `ReviewEvent`, `SchedulerState`, and `UserSettings`.
- **Hilt DI**: `DatabaseModule` provides `AppDatabase`, `HifzDao`, `HifzScheduler`, and binds `HifzRepository`.

## Verification Results
- **Build**: Successfully built with `assembleDebug`.
- **Unit Tests**: 6 tests passed in `:scheduler:test` covering the scheduler logic.

## Technical Notes
- **AGP 9.3.1**: Used the new built-in Kotlin support (removed `kotlin.android` plugin).
- **Hilt 2.60.1**: Integrated for dependency injection.
- **Room 2.7.0**: Integrated for local storage.
- **Java 11**: Set as the target compatibility for all modules.
