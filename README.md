# Photo-Organizer

An Android app that helps keeping a photo library tidy, a few minutes at a time.

Once a day it reminds you to review the photos and videos of **this day**: everything
captured today and on the same calendar day in all previous years. On March 3rd, 2026 that
is March 3rd of 2026, 2025, 2024 and so on.

## Features

- **Daily reminder** at a configurable time (default 20:00). It stays quiet if the day has
  already been reviewed or has no media.
- **Day overview** of today's media, grouped by year. Other days can be reached by paging
  back and forth or picking a date; tapping a photo starts the review with it.
- **Folder selection**: the review can be limited to certain folders, for example only the
  camera folder, leaving out images received through messengers.
- **Swipe review**: one item at a time, swipe left to delete, right to keep. Items can also
  be marked as favorite or moved to an album; every decision can be undone. Photos can be
  zoomed by pinching or double tapping to check their details.
- **Browse before deciding**: tapping the right or left side of a photo shows the next or
  previous one without deciding; decided photos can be revisited and changed.
- **Series of similar photos**: photos taken within seconds of each other are offered side by
  side, so the best one can be picked and the rest discarded in one step.
- **Nothing changes until you confirm**: decisions are collected and applied together at the
  end. Deleted items go to the system trash, where Android keeps them for 30 days.

Everything happens on the device. The app has no network permission.

## Building

Requirements: JDK 17 or newer and the Android SDK (Android Studio brings both).

```sh
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:installDebug         # install it on a connected device
./gradlew :app:testDebugUnitTest    # run the unit tests
./gradlew :app:lintDebug            # run Android lint
```

The app requires Android 13 (API 33) or newer.

## Architecture

A single `app` module, written in Kotlin with Jetpack Compose and split into layers by package:

| Package    | Responsibility                                                                 |
|------------|--------------------------------------------------------------------------------|
| `domain`   | Plain Kotlin without Android dependencies: models, date logic, the review session and the detection of photo series. Covered by unit tests. |
| `data`     | `MediaRepository` (MediaStore), `SettingsRepository` (DataStore) and `ReviewMediaSource`, which combines the two into "the media to review on a day". |
| `reminder` | Alarm scheduling, the broadcast receivers and the notification.                |
| `ui`       | One package per screen (`home`, `review`, `settings`, `folders`), each with a composable screen and a `ViewModel` exposing a single state flow, plus shared pieces in `common` and the routes in `navigation`. |
| `di`       | Hilt module wiring the above together.                                         |

Dependencies point inwards: `ui` and `reminder` use `data` and `domain`, `data` uses
`domain`, and `domain` uses nothing.

Some decisions worth knowing about:

- **Changes are applied in batches.** Android asks the user to confirm every modification of
  media an app did not create. Confirming each photo separately would make swiping
  pointless, so `ReviewSession` only records decisions and turns them into a `ChangePlan`,
  which needs one system dialog per kind of change (trash, favorite, move).
- **Albums are folders.** Moving an item changes its `RELATIVE_PATH` in MediaStore, which
  moves the file. New albums are created below `Pictures/`.
- **The reminder is an inexact alarm** (`AlarmManager.setAndAllowWhileIdle`), re-armed every
  time it fires and after reboots or time changes. Android may deliver it up to an hour
  late to save battery, but it needs no special permission.

- **The review can be browsed freely.** `ReviewSession` keeps a movable position and one
  decision per item, so photos can be compared and decisions revised before anything is applied.

`CLAUDE.md` has more detailed notes for development, including how the app is tested on an
emulator with the helpers in `tools/emulator`.

Library versions are managed in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
