# ProTech Music — Two-Phase / 15-Task Release Closure Plan
Version: 3.0 | 2026-11-21 | Root: E:\ProTech\ProTechMusic
User approval: yes, requested two minimal sequential phases of 15 actionable steps each.

## Progress metric contract
- Official *overall completion* keeps original 25 weighted roadmap tasks (each 4%): last verified baseline 28%, 7/25 PASS.
- The 30 closure subtasks below are independently measured per phase as PASS/15; they do not overwrite or artificially increase the official 25-task overall percent. Final 100% requires all 25 tasks with real evidence.
- Each subtask records status PLANNED/IN_PROGRESS/PASS/BLOCKED; PASS requires artifacts in docs/QA* and actual builds/tests where applicable.
- Automatically go to the next independent subtask after PASS; record BLOCKED and continue independent work. Never claim 100% if physical phone, code signing, versions or publication tests cannot be completed.
- Do not delete user files for testing. All destructive QA uses generated fixture audio, with both app and Android confirmation.

## Phase 1 — Feature-complete release candidate, exactly 15 steps
| ID | Deliverable | Acceptance gate |
|---|---|---|
| F01 | Preserve local Git, backup release and inspect current source | Clean baseline, handoff and SHA |
| F02 | Persist accurate play counts and last-played with schema migration | Deterministic tests for short plays/skips/duplicates and non-destructive schema upgrades |
| F03 | Smart lists: favorites, recently added, recently played, most played | Group lists derived from real data; no fabricated analytics |
| F04 | Smart lists UI with sort, search and pagination | Works with Arabic RTL and narrow viewport |
| F05 | Music discovery by WhatsApp/Downloads/Telegram/Recordings/Music and folders | MediaStore audio, SAF files and permissions tested |
| F06 | Manual playlists CRUD/add one or many/reorder/remove without file deletion | UI emulator + DB persistence checks |
| F07 | End-to-end selection: single/any/all, add/share/delete | Android Sharesheet 1,N; confirmation of deletion |
| F08 | Playback next/previous, ordered/shuffle, playlist-only and repeat 0/1/all | Actual automatic track transitions and persistence |
| F09 | Non-destructive remove vs permanent MediaStore delete + deny/allow | Android consent, denied leaves file intact, approved fixture disappears |
| F10 | SAF/M3U8/JSON backup/migration and missing audio repair | Roundtrip and malformed input tests |
| F11 | Audio effects: EQ, bass, loudness bound, safe fallbacks and player speed | No distortion claims without physical hardware tests |
| F12 | Media art, lyrics .lrc and queue/notifications/audio focus | No blank-state crashes, content URIs validated |
| F13 | New UI: bottom insets, min 48dp touches, large font, RTL, landscape | 320dp/411dp, gesture and 3-button screenshot evidence |
| F14 | 1k/10k/50k audio indexing and background battery/memory QA | Timed measurements and regressions, not estimates |
| F15 | RC assembly and unit/instrumentation suite | Build PASS; issue list zero P0/P1 for RC |
Phase 1 gate: RC is not a public release. If F11 hardware QA or F14 stress QA unavailable, clearly mark BLOCKED and continue to Phase 2 independent tasks.

## Phase 2 — Production validation and release, exactly 15 steps
| ID | Deliverable | Acceptance gate |
|---|---|---|
| R01 | Complete release audit of roadmap and RC changelog | Every feature cross-referenced to test |
| R02 | Android API 26–28 and scoped storage legacy checks | Emulator evidence or BLOCKED |
| R03 | Android API 29–32 MediaStore/SAF/delete regression | Emulator evidence or BLOCKED |
| R04 | Android API 33,34,35,36 permissions and foreground service | Emulator evidence or BLOCKED |
| R05 | Device-specific WhatsApp voice note and Downloads discovery | Real device or fixture evidence with limitations |
| R06 | Real hardware headset/Bluetooth calls/media buttons | QA record or BLOCKED |
| R07 | Playback long-session, gapless, shuffle and repeat boundary matrix | Automated/media-session evidence |
| R08 | Safe delete test matrix: cancel, deny, consent, batch, missing URI, SAF | Only generated fixtures, never unapproved files |
| R09 | Accessibility: Arabic/English, RTL/LTR, TalkBack, 160%+ font | Screenshot/UI evidence |
| R10 | Large library stress, latency and memory regression | Repeatable perf logs and thresholds |
| R11 | Privacy, permission minimization and error audit | No raw file paths/shared data leakage |
| R12 | Installer/icon/adaptive icon and proper branding | Verified on emulator/home screen |
| R13 | Signed stable APK/AAB, version, SHA256, reproducible clean build | Safe persistent keystore, verified signature |
| R14 | GitHub release and alghad.digital tested download link | Actual live links with version/hash confirmed |
| R15 | Final end-to-end acceptance and 100% evidence reconciliation | All original 25 tasks PASS; zero P0/P1; signed release |
Release gate: NEVER publish a DEBUG build as final, nor mark R15 PASS without signed release and live verification. Keep progress and log after each tool session.

## Previously verified evidence (preserved)
- v0.3.4 debug (commit 2458e60), 14/14 unit tests, Android 15 emulator.
- Audio automatic advance, ordered/shuffle within QAPlaylist, selected 10 sent to Android Sharesheet, 2 inserted into playlist.
- Permanent deletion of synthetic WAV after app confirmation and Android OS consent; cancellation/denial preserved file.
- Android bottom safe area and M3U8 actual SAF roundtrip.
- docs/QA_V034_PLAYBACK_BULK_DELETE_2026-11-21.md and docs/HANDOFF_V10.json.
