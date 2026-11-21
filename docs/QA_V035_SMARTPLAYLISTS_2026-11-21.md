# ProTech Music v0.3.5 — Phase F smart lists and play-stat evidence
Date: 2026-11-21, Asia/Riyadh
Android 15 API35 emulator-5554, authorized Windows host DESKTOP-PM75RQM
Release state: DEBUG/QA, not production release.

## Code delivered
- Upgraded existing SQLite database user_version 1 -> 2 with new play_stats table and last-played index. Existing playlists, entries, favorites and imported audio tables not deleted.
- Added MediaStore.DATE_ADDED to AudioTrack, and timestamps to manually imported SAF media, without requiring new storage permissions.
- Added ListeningTracker with monotonic time, accumulated audible segments, one count per qualified playback instance, 0.75s min for short audio, 15s max threshold for longer audio. Skips/seeks alone do not count. Works across pauses and natural transitions.
- MusicService persists qualified playback asynchronously and increments Compose statsRevision to refresh UI.
- Smart lists: recently added, recently played, most played (sorted by true play_count), favorites; search and bulk operations preserved. Most played row shows count, recently played row shows date/time.
- Smart UI heading shortened and single line to prevent clipped RTL text; responsive horizontal filter chips, existing bottom WindowInsets.navBars protection and 48dp action areas retained.
- Files changed: AudioRepository.kt, ListeningTracker.kt, MusicStore.kt, MusicService.kt, MainActivity.kt, app/build.gradle.kts, new ListeningTrackerTest.kt.

## Test results
- docs/build_v035_smartlists.log: BUILD SUCCESSFUL :app:assembleDebug :app:testDebugUnitTest.
- docs/build_v035_ui_final.log: BUILD SUCCESSFUL again after display refinements.
- Unit tests: ListeningTrackerTest 8 PASS, M3uCodecTest 7 PASS, SourceClassifierTest 7 PASS => 22 PASS, 0 failures/errors.
- Installed v0.3.5 debug APK with adb install -r, retaining app data, on Android 15 emulator. Pre-upgrade database user_version=1, playlists=3, entries=5; post-upgrade user_version=2, playlists=3, entries=5; no data loss in this test.
- Before playback play_stats had 0 rows. Started test 2-second WAV qa_v034_auto_01. After playback Android sqlite3 reported URI content://media/external/audio/media/1000000170 with play_count=1 and a valid last_played epoch millisecond timestamp; no app fatal exception. This is a real Android MediaStore fixture audio event, not a fabricated stat.
- Switched to recently played view, saw just that audio from play_stats. Screenshot qa/smart_recently_played_v035.png.
- Switched to most played view; saw qa_v034_auto_01 and explicit "1 تشغيل" in metadata. Screenshot qa/smart_most_v035_final.png.
- Recently added smart list used MediaStore DATE_ADDED and displayed source-diverse audio items. qa/smart_recent_added_v035.png and qa/smart_new_ui_035.png.
- Portrait responsive QA on 720x1600 pixels at density 360 (~320dp), font_scale 1.3, navigation gesture; smart-list title and filter chips readable, compact player/nav above Android system gesture handle. Screenshot qa/smart_320_final.png.
- Existing safe media deletion, selected share and playlist scopes from v0.3.4 unchanged; QA evidence docs/QA_V034_PLAYBACK_BULK_DELETE_2026-11-21.md.
- Existing Kotlin Notification.Action.Builder three deprecation warnings remain (non-blocking), flagged for Release closure step.

## Limits and what is NOT passed
- Only Android 15 emulator verified for v0.3.5. Other Android 8/10/11/13/14/16, large-library performance, TalkBack, landscape, real hardware Bluetooth/audio effects/delete flows, production signing and website/GitHub release remain open.
- Recent additions currently sorts all accessible songs by MediaStore timestamp; no cutoff limit imposed. Missing/invalid timestamp goes last.
- On abrupt process kill, pending async play-stat DB write may not be durable; lifecycle hardening planned.
- All testing was with synthetic emulator audio and its existing fixture data; no user device media deleted.
