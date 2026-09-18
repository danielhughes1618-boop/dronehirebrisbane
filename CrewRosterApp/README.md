# Crew Roster (Android)

A native Android app (Kotlin + Jetpack Compose) that allocates a 12-person team across 3
vehicles each morning, with fair driver rotation, pinning, manual swap/change-driver,
history, and a driving tally. All data is stored locally on the device (Jetpack DataStore) -
no server, no account, works fully offline.

This is the native rewrite of an earlier single-file HTML prototype (`index.html` at the repo
root, kept for reference/fallback) - same rules, same allocation algorithm, now a real
installable app with its own icon.

## Project structure

- `core/` - a plain Kotlin/JVM module (no Android dependency) with the data models and the
  allocation algorithm (`AllocationEngine.kt`). This is unit-tested with JUnit
  (`core/src/test/...`) and can be built/tested on any machine with just a JDK - no Android
  SDK needed for this module.
- `app/` - the Android application: Jetpack Compose UI, a `CrewRosterViewModel`, and
  persistence via DataStore.

## Before you open this in Android Studio

You'll need:

1. **Android Studio** (the current stable release) - free, from
   [developer.android.com/studio](https://developer.android.com/studio).
2. A phone with **Developer Options** and **USB debugging** turned on, if you want to run
   straight to your device (see below). Not required if you only want an APK file to sideload.

This project was authored without access to an Android SDK or emulator, so it has **not**
been compiled by a real Android toolchain yet - only the `core` module's logic has been
compiled and tested (with plain JVM/Gradle, no Android SDK required for that part). When you
open it, Android Studio may prompt you to:

- **Upgrade the Gradle/Android Gradle Plugin version** - accept it, this is normal.
- **Sync/download SDK components** (a specific compileSdk/build-tools version) - let it.
- Show a handful of small warnings/errors on first sync - most of these are one-click "Apply
  Suggestion" fixes in Android Studio (e.g. a dependency version bump). If you hit one you're
  not sure about, screenshot it and ask - the whole reason for this note is that this code has
  had a very thorough manual and multi-pass AI review in place of a real compiler, but a real
  compiler is still the ground truth.

## Run it on your phone via USB debugging (recommended)

1. On your phone: **Settings → About phone**, tap **Build number** 7 times to unlock Developer
   Options. Then **Settings → System → Developer options → USB debugging → on**.
2. Plug the phone into your Windows PC with a USB cable. Allow the "Allow USB debugging?"
   prompt on the phone.
3. Open this `CrewRosterApp` folder in Android Studio (**File → Open**, pick the
   `CrewRosterApp` folder itself, not the repo root).
4. Let Gradle sync finish (first sync downloads a lot - can take several minutes).
5. Your phone should appear in the device dropdown in the toolbar (next to the Run button). If
   it doesn't, check the cable/USB debugging prompt.
6. Click the green **Run ▶** button. Android Studio builds the app, installs it, and launches
   it on your phone automatically.

That's it - no separate "install" step, no APK to move around by hand.

## Or: build a plain .apk file to sideload

1. Open the project in Android Studio as above and let it sync.
2. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. When it finishes, click the "locate" link in the notification (or find it under
   `app/build/outputs/apk/debug/app-debug.apk`).
4. Copy that file to your phone (email, USB transfer, cloud drive - whatever's easiest) and
   open it there. Android will ask you to allow installing from that source the first time;
   allow it, then install.

A debug APK built this way works fine for personal use; it just isn't signed for the Play
Store.

## Notes / design decisions

- **Persistence**: everything (people, vehicles, history, the current day's draft) is stored
  as one JSON blob in Jetpack DataStore - deliberately simple (no database) since the data
  volume is small and this mirrors the exact data shape already validated in the HTML
  prototype.
- **"Yesterday"** for driver-rotation fairness means the most recently *confirmed* day in
  history, not literal calendar-yesterday - so it still behaves sensibly across weekends or
  days the app wasn't used.
- **CSV export** uses Android's share sheet (rather than a raw file download) so you can send
  it straight to email, Drive, etc.
- If you rename this project/package later, the applicationId is `com.crewroster.app` -
  update `app/build.gradle.kts` and the FileProvider authority strings together if you change it.
