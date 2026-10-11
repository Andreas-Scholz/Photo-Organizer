# Photo-Organizer – notes for working on this repository

Android app for tidying a photo library a little every day. See `README.md` for the feature
list and the architecture overview; this file holds what is needed to continue development.

## Working agreements

- **Never operate the app on a physical device.** A connected phone holds the owner's real
  photos. Installing a new build on it is fine (`adb -s <serial> install -r …`); launching,
  tapping or anything that could delete or change media is not. All interactive testing
  happens on an emulator with generated photos (see "Testing on an emulator").
- **One branch per feature or fix**, merged into `main` with `--no-ff` when finished and
  verified, then `main` is pushed. No pull requests unless asked.
- Code, comments, commit messages and docs are in English. UI texts exist in English
  (`values/strings.xml`, default) and German (`values-de/strings.xml`); add both.
- Commits are small and logical, with a body explaining the why.
- Keep `README.md` in step with features and architecture.

## Commands

```sh
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # JVM unit tests
./gradlew :app:lintDebug            # report: app/build/reports/lint-results-debug.txt
```

Run all three before merging. Three lint warnings are known and accepted: `OldTargetApi`
(targetSdk 36 is deliberate, behavior on 37 is untested), `InflateParams` (the video player
layout is inflated without a parent for `AndroidView`) and `UnsafeProtectedBroadcastReceiver`
(the receiver is not exported and its work is idempotent).

`./gradlew :app:installDebug` installs on **every** connected device. With a phone attached,
install explicitly per device instead.

After renaming or deleting resource folders, an incremental build can fail with stale
"resource not found" errors; `./gradlew :app:clean` fixes that.

## Stack

Kotlin, Jetpack Compose (Material 3), Hilt (KSP), Navigation Compose with type-safe routes,
DataStore Preferences, Coil 3 (with `coil-video` for video thumbnails), Media3 ExoPlayer.
AGP 9 with built-in Kotlin (no `kotlin-android` plugin), Gradle 9 wrapper, version catalog in
`gradle/libs.versions.toml`. `minSdk 33`, `compileSdk 37`, `targetSdk 36`.

No icon library: icons are vector drawables in `res/drawable/ic_*.xml`, used through
`painterResource`.

## Code map

Package root: `app/src/main/java/io/github/andreasscholz/photoorganizer/`

- `domain/` – plain Kotlin, no Android imports, unit tested.
  - `OnThisDay.kt` – `anniversariesOf`, `anniversaryRangesOf` (the day in all years as
    millisecond ranges for the query), `reviewDayOf` (maps any picked date to the same
    calendar day in the current year).
  - `ReminderTime.kt` – `nextOccurrenceOf`.
  - `model/` – `MediaItem`, `Album` (a folder to move into), `MediaFolder`, `FolderFilter`.
  - `review/ReviewSession.kt` – the heart of the review, an immutable value: the items, a
    freely movable `index`, at most one `Decision` per item (ordered by when it was made) and
    favorite changes kept separately. `decide` moves on to the next open item, `undo` takes
    back the latest decision, the session is finished when every item is decided.
    `toChangePlan()` produces the `ChangePlan`.
  - `review/PhotoSeries.kt` – `findSeries`: photos within 30 s of the previous one.
- `data/`
  - `media/MediaRepository.kt` + `MediaStoreMediaRepository.kt` – all MediaStore access:
    the day query, folders/albums, and the trash/favorite/write requests. Also the
    `MediaItem.contentUri` extension.
  - `media/MediaAccess.kt` – full, partial (Android 14 "selected photos") or no permission.
  - `settings/SettingsRepository.kt` – reminder settings, folder filter, completed days.
  - `ReviewMediaSource.kt` – "media of a day, honoring the folder filter"; used by the
    home screen, the review and the reminder, so they always agree.
- `reminder/` – `ReminderScheduler` (AlarmManager), `ReminderReceiver` and
  `RescheduleReceiver` (boot, time changes), `DailyReminder` (decides whether to notify),
  `ReminderNotifier`.
- `ui/` – one package per screen with a composable and a Hilt `ViewModel` exposing one
  `StateFlow` of a UI state data class.
  - `home/` – day overview with day paging and date picker; tapping a thumbnail starts the
    review at that item.
  - `review/` – `ReviewScreen` (card, actions, discard dialog), `SwipeableCard`,
    `MediaViewer` (zoomable photo, video player, tap zones), `SeriesStrip`, `AlbumPicker`,
    `ReviewSummary`, `ReviewViewModel` (also runs the apply steps).
  - `settings/`, `folders/` – reminder settings and folder selection.
  - `common/` – `Zoomable.kt` (`ZoomState`, `ZoomableBox`), `MediaThumbnail`, date formatting.
  - `navigation/` – `Routes.kt` (serializable route objects) and `AppNavHost`.
- `di/AppModule.kt` – bindings, plus the `@IoDispatcher` and `@ApplicationScope` qualifiers.

## Things that are not obvious

**Media dates.** `DATE_TAKEN` is often null: for screenshots and messenger images, and for
photos whose EXIF has no time zone offset unless the file was modified within a day of the
capture time. The query therefore falls back to `DATE_MODIFIED` (which survives a phone
migration, unlike `DATE_ADDED`). Such media can show up under the wrong day.

**Changing media needs user consent.** Trash, favorite and move go through
`MediaStore.create…Request`, each showing a system dialog. That is why the review only
collects decisions and `ReviewViewModel` applies them at the end as a queue of `ChangeStep`s,
one dialog per kind of change. After a declined dialog the user can continue where they
left off; steps that already ran are not repeated. Moving means updating `RELATIVE_PATH` after a write
request; new albums go below `Pictures/`.

**Gestures in the review card** are layered and easy to break:
- `SwipeableCard` (outer) uses `detectHorizontalDragGestures`. A swipe counts when dragged
  past 30 % of the width or released as a flick in the direction the card was moved.
- `ZoomableBox` (inner) only consumes pointer events during a pinch or while zoomed, so
  single-finger drags reach the swipe at normal zoom. While zoomed, swiping is off on purpose.
- `ZoomState.settle()` snaps a barely zoomed photo (< 115 %) back when the fingers lift.
  Without it, pinching out leaves the photo at ~102 % and swiping silently stops working –
  this was a real bug.
- Single taps arrive about 0.3 s late because they could become a double tap (zoom). Taps in
  the left/right third browse; the middle third does nothing on photos and pauses videos.
- The video player uses a `TextureView` (set in `res/layout/video_player.xml`, the only way
  to choose it) because a `SurfaceView` stays black inside the clipped, transformed card. Its
  built-in controller is disabled; a Compose layer on top handles taps, otherwise the player
  view swallows the swipes.

**Dates in the UI** must be formatted with the locale from `LocalConfiguration`
(`ui/common/DateFormatting.kt`), not `Locale.getDefault()`, or per-app languages are ignored.

**Completed days** are stored as a set of ISO dates (`completed_days`); the legacy single
`last_completed_day` key is still read. A day is completed when its review was applied.

**The reminder** uses `setAndAllowWhileIdle`, which Android may deliver up to an hour late.
Exact alarms were avoided because they need a special permission.

**Review days and years.** The shown day is always normalized to the current year
(`reviewDayOf`), so its anniversaries reach up to today. February 29th maps to the latest
leap year and is only reachable through the date picker in other years.

## Testing on an emulator

There is no instrumentation test suite; UI behavior is checked by driving an emulator
through `adb`. Helpers live in `tools/emulator/`.

1. Create and start a headless AVD (API 34 `google_apis` image works; `minSdk` is 33):
   `avdmanager create avd -n photo_organizer_test -k "system-images;android-34;google_apis;x86_64"`,
   then `emulator -avd photo_organizer_test -no-window -no-audio -no-snapshot -gpu swiftshader_indirect -skin 1080x2400`.
   Delete the AVD again when done.
2. Generate photos and push them:
   `python tools/emulator/gen_test_media.py <dir>`, `adb -s emulator-5554 push <dir>/. /sdcard/DCIM/Camera/`.
   A test video: `adb shell screenrecord --time-limit 3 /sdcard/DCIM/Camera/VID_today.mp4`.
   Then index them: `adb shell content call --uri content://media --method scan_volume --arg external_primary`.
   Put copies into other folders (e.g. a WhatsApp path) to test the folder filter.
3. Install with `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`
   and grant `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS` via `pm grant`
   to skip the dialogs.
4. Drive the UI with `tools/emulator/ui.sh dump|tap|shot` and `adb shell input tap/swipe`.

Limits of this setup, learned the hard way:
- `adb shell input` cannot do multi-touch, so pinch zoom is only covered by `ZoomStateTest`.
- A double tap needs two overlapping commands:
  `adb shell "input tap X Y & sleep 0.12; input tap X Y; wait"`.
- Injected fast swipes sometimes lose their move events right after a cold start; that is an
  artifact, not an app bug. Real problems reported from the phone may not reproduce here.
- The reminder can be triggered with `adb root` and
  `adb shell am broadcast -n <package>/.reminder.ReminderReceiver`; `dumpsys alarm` shows
  the pending alarm.

## Ideas discussed but not built

- Exact alarm for the reminder (needs a special permission).
- Detecting series by image similarity instead of time only; a configurable time gap.
- Seek bar for videos (the player controls were removed in favor of tap to pause).
- Including February 29th when reviewing February 28th in non-leap years.
- Skipping the confirmation dialogs via the `MANAGE_MEDIA` special access.
