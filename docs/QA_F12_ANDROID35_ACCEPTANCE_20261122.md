# ProTech Music F12 Acceptance — Android 15, 2026-11-22

Status: **PASS for F12 emulator feature gate**. The original 25-task overall milestone remains **44% (11/25)**; F12 closing is a separate, smaller acceptance subtask and does not independently satisfy original tasks 17 or 20 or the final release gate.

## Target and safety
- Machine DESKTOP-PM75RQM, AVD `ProTechMusicQA35_211126`, Android 15/API35, ADB emulator-5554.
- User's files were not touched. Fixture generation script: `docs/QA_F12_GENERATE_FIXTURES.ps1`. It creates two synthetic short MP3 files, a local embedded JPEG cover, and a 3-line LRC under ignored `qa/fixtures/f12`. Test files pushed to the emulator's shared Music and Download directories.
- Feature-specific main source: `CoverArt.kt`, `LrcLyrics.kt`, `LyricsStore.kt`, `MainActivity.kt`, `MusicService.kt`.

## Reproducible evidence
1. Offline build `assembleDebug` and `testDebugUnitTest` succeeded, with **48 tests / 0 failures**. Local log: `docs/build_f12_f13_followup_20261122.log`.
2. Debug APK installed on API35 emulator using `adb install -r` (Success); app launched through `am start`, application PID recorded and no app FATAL EXCEPTION in inspected logcat window.
3. MediaStore listed seeded `ProTech QA A` and `ProTech QA B` tracks. Screenshot: `docs/evidence/f12/f12_audio_seeded_2.png`.
4. Embedded blue JPEG album art was decoded and displayed in Now Playing, with a bounded decoding path on IO dispatcher. Screenshot: `docs/evidence/f12/f12_new_player_modal.png`.
5. MediaSession reported `state=PLAYING`, description `ProTech QA A`, and `metadata: size=4` (including artwork) vs size=3 without artwork.
6. After 9 seconds, automatic queue advancement showed a different track `qa_v034_auto_02`, still PLAYING; before that `ProTech QA A` was PLAYING. See `docs/QA_F12_ANDROID35_EVIDENCE_20261122.log`. This tests transitions using existing synthetic QA audio plus newly seeded files.
7. Android SAF OpenDocument picker opened Downloads and accepted `protech-qa-a.lrc`. App private storage contained a hashed, 72-byte `.lrc` with correct ownership; UI displayed `QA First line` and a 3-line import success toast. Screenshot: `docs/evidence/f12/f12_lyrics_imported.png`. JUnit `LrcLyricsTest` verifies timestamp ordering and time-based selection.
8. The playback foreground notification `id=109` was displayed with actions Previous/Pause/Next and title `ProTech QA A`. Android audio focus stack recorded package `digital.alghad.protechmusic` with `GAIN`, `USAGE_MEDIA`, `loss:none`. See `docs/QA_F12_NOTIFICATION_FOCUS_20261122.log`.
9. The Now Playing bottom sheet is scrollable with bounded viewport height and 48dp-min media controls; visible in the artwork screenshot.

## Limitations / pending QA
- F12 is an **emulator feature gate**, not an all-device certification. Real phone, wired/Bluetooth headset, incoming calls, Android 8–16 matrix, TalkBack, battery and 10k–50k library stress remain pending.
- F13 screen matrix 320dp/411dp, landscape, gesture/three-button, large-font and EN LTR remains open; F14/F15 and R gates remain open.
- Original task 20 requires additional gestures and one-tap playlist/favorite actions; do not mark that whole original task PASS solely on F12.
- Do not publish a debug build as a stable release. A signed, explicitly labeled Beta/Preview can be published separately after signature, checksum and live download checks.
