# ProTech Music — V27 QA and signed preview ledger (2026-11-22)

## Source changes
- `MusicService.kt`: persist repeat OFF / ONE / ALL in playback SharedPreferences; restore repeat on MusicService creation prior to queue restore.
- `RcPlaybackHarness.kt`: in the Android emulator integration test assert Media3 repeat mode and persisted state for each transition; retain 0→1→2 actual WAV auto-next test.
- `app/build.gradle.kts`: v0.4.5-beta.7, versionCode=18.
- Existing non-destructive playlist and MediaStore behavior preserved; private keystore and QA audio fixture archives excluded from publication.

## Independent signed-upgrade QA performed
- Isolated API35 AVD: `ProTechMusicReleaseQA35_V26` (`emulator-5556`).
- Beta5 signed APK, versionCode16: installed using `adb install --no-streaming -r`; launched.
- Beta6 signed APK, versionCode17: installed over Beta5 **without uninstall**, exit success. `dumpsys package` confirmed code17; screenshots saved to `docs/evidence/v26/signed_upgrade/`.
- Android `Pixel Launcher isn't responding` and `System UI isn't responding` dialogs were captured in actual UI hierarchy. Therefore no claim that Beta6 compact UI or language preference survived was made. Only **package-level signed in-place upgrade** is proven.
- The QA AVD was stopped after collecting evidence without deleting any personal media.

## Incomplete gates not to inflate
- A06 (10k actual app-visible files), A07 (50k actual app-visible files), A08, A09, A10, A11, A12, A13, A15; Stage A remains 6/15 (40%).
- Stage B is 0/15, despite incremental evidence supporting B07 and B12; full production acceptance needs separate tests.
- Original project 11/25 (44%), feature phase F 13/15 (86.7%), production R 0/15.
- Physical hardware phone is not connected over ADB; calibrated battery, headset/Bluetooth/calls unverified.
- On previous Android 15 QA, 10k shell-created WAV files were `IS_PENDING` and invisible to the app; do not count them as validated real scans.

## Build, signing, release
- **PASS** Gradle `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` on 2026-11-22: `BUILD SUCCESSFUL in 32m 50s`. Detailed log: `docs/build_v045_beta7_qa_retry.log`.
- **PASS** JVM JUnit XML: 9 suites, **54 tests / 0 failures / 0 errors**. Debug app APK and androidTest APK were built and verified on disk. The new instrumentation was **compiled, not yet executed on the emulator** (device System UI ANR in prior tests).
- **PENDING** signed Beta7 Release `assembleRelease`, existing-key v2/v3 signing, SHA, GitHub prerelease and bilingual live site audit. Never mark these PASS without artifact evidence.

## Resumption
- Source: `E:\ProTech\ProTechMusic`; plan: `docs/PLAN_REMAINING_V27_2_PHASES_20261122.md`; prior handoff: `docs/HANDOFF_V26.json`.
- Never publish Debug APK or claim stable/production while physical-device and real large-library gates remain.

## Preliminary security/permission audit (not B11 PASS)
- Main manifest requests READ_MEDIA_AUDIO; READ_EXTERNAL_STORAGE restricted to maxSdk32; WRITE_EXTERNAL_STORAGE restricted to maxSdk28; foreground media playback and notifications. No INTERNET, READ_SMS or READ_CONTACTS permissions declared.
- Playback service is non-exported; QA activities require android.permission.DUMP and are only in debug manifest.
- Android system backup is enabled (`android:allowBackup="true"`). Before stable release, verify that private playlist metadata and persisted SAF URIs are appropriately excluded/protected; this remains an audit item rather than silently changing the user's backup behavior.
