# ProTech Music — Android Development Roadmap
Version: 1.0
Date: 2026-11-21
Owner: ProTech / ALGHAD DIGITAL
Status: PLANNED — implementation 0% (branding icon concept created separately)
Project root: E:\ProTech\ProTechMusic
Language: Arabic RTL by default, English optionally
Platform: Android, offline-first (minSdk 26 proposed; targetSdk to match current requirements at release)
License: All Rights Reserved (to be confirmed before public release)

## Product definition
Native offline music player that automatically indexes all user-accessible shared audio media after obtaining permission. It supports customized playlists and background playback, without copying or uploading media to servers. It must never claim to access protected/private app storage or files excluded from Android media indexing. Users may manually add audio via Storage Access Framework if necessary.

## Architecture
- Kotlin + Jetpack Compose + Material 3 + Navigation; single-activity UI; MVVM + Repository.
- Media3 ExoPlayer + MediaSessionService (consider MediaLibraryService for system browsing/Android Auto later).
- MediaStore.Audio + ContentResolver + change observers / incremental sync.
- Room for playlists, ordered membership, favorites, playback statistics and cached metadata; DataStore for settings.
- Kotlin Coroutines/Flow, WorkManager for deferred reconciliation when appropriate.
- Separate modules/packages: core, media-index, playback, library, playlists, design-system, settings, tests.
- Never hold large bitmap blobs in Room. Use artwork URI and properly bounded image cache.
- Files referenced by content URI; preserve storage volume identity, handle stale URIs, migrations and reinstalls.

## Phase A — Foundation and design (5 tasks, 20%)
- [ ] 01 (4%) Requirements baseline: supported Android versions, local-only behavior, supported controls, export policy, documented acceptance criteria.
- [ ] 02 (4%) Initialize Gradle Kotlin Android project, lint, static analysis, version catalog, CI build, reproducible debug APK.
- [ ] 03 (4%) Implement accessible design system; dark/light, cyan/violet accents, Arabic RTL/English, large touch targets, dynamic type.
- [ ] 04 (4%) Implement navigation wireframes: Home, All Tracks, Albums, Artists, Folders, Playlists, Queue, Now Playing, Settings.
- [ ] 05 (4%) Design Room schema and migrations for tracks/index cache, playlists, playlist_items(position), favorites and playback state.
Gate A: clean build, RTL UI rendered, schema migrations tested, screenshots saved.

## Phase B — Automatic device library (5 tasks, 20%)
- [ ] 06 (4%) Permissions UX: READ_MEDIA_AUDIO for Android 13+, version-appropriate legacy access, graceful denial, SAF manual picker.
- [ ] 07 (4%) Initial MediaStore scan of accessible music across available volumes; configurable inclusion of short clips/recordings and ringtone exclusions.
- [ ] 08 (4%) Incremental sync on app start/resume and media changes; move/rename/delete detection, scoped work, no costly full-rescan loop.
- [ ] 09 (4%) Extract metadata safely (title, artist, album, album artist, genre, duration, disc, year, artwork) with fallbacks for damaged tags.
- [ ] 10 (4%) Fast search and filtering across Arabic/English names and file paths, sorting, folder browsing, reliable zero-results/permission states.
Gate B: newly added track appears without manual import, removed track disappears, no duplicates across volumes, >10,000 tracks responsive.

## Phase C — Playlists and organization (5 tasks, 20%)
- [ ] 11 (4%) Playlist CRUD: create/rename/duplicate/delete playlist, user-selected cover, unlimited named playlists within device resource limits.
- [ ] 12 (4%) Add single/multiple/all selected tracks to playlists; remove from playlist without touching source file; manual drag reorder, no unintended duplicates.
- [ ] 13 (4%) Favorites + smart playlists (recently added, recently played, most played, by artist/album/genre) with correct stats semantics.
- [ ] 14 (4%) Persistent play queue: play next, enqueue, reorder, resume queue; M3U/M3U8 import/export with SAF and graceful missing-item reports.
- [ ] 15 (4%) Export/import backup of app metadata and playlists (JSON plus M3U8 where appropriate); restore across path changes without exporting copyrighted audio.
Gate C: playlists and ordering survive process death/reboot; export/import round trip; deletion never removes original file.

## Phase D — Playback and advanced features (5 tasks, 20%)
- [ ] 16 (4%) Media3 engine: play/pause/seek/next/previous, shuffle, repeat one/all/off, robust format error handling and audio focus.
- [ ] 17 (4%) MediaSessionService: background playback, media notification, lock screen, headphone/Bluetooth controls, resume support, headset-disconnect handling.
- [ ] 18 (4%) Sleep timer, playback speed, bookmarks/resume, gapless where technically possible, history and listening statistics.
- [ ] 19 (4%) Equalizer/presets/bass when supported by device audio effects; graceful fallback; optional crossfade only after real audio-mixing performance proof.
- [ ] 20 (4%) Full Now Playing view: artwork, swipe gestures, seek bar, synced local .lrc lyric display when present, favorites and one-tap playlist actions.
Gate D: stable hour-long background session, calls/notifications/audio focus handled correctly, no ANRs/crashes, supported features correctly detected.

## Phase E — Quality, release, handoff (5 tasks, 20%)
- [ ] 21 (4%) Performance tests at 1k/10k/50k tracks; indexing off UI thread, paging/virtualization, bounded caches, battery/memory profiling.
- [ ] 22 (4%) Automated unit tests: playlist order, import/export, smart rules, incremental index reconciliation, migrations; UI tests for RTL and key flows.
- [ ] 23 (4%) Instrumented real-device/emulator matrix Android 8, 11, 13, 14, 15, 16 where available; media volumes, Bluetooth, offline and background restrictions.
- [ ] 24 (4%) Accessibility/security/privacy review: permissions minimized, no sensitive analytics, app data backup safety, media delete confirmation, contrast/focus.
- [ ] 25 (4%) Signed release APK/AAB, versioning, changelog, icon assets (adaptive foreground/background + monochrome), license, GitHub repository, website publication only after QA gate.
Gate E: all automated checks green, manual critical user journeys evidenced, release APK installed and exercised, SHA-256/checksums documented.

## Core acceptance tests
1. Fresh install + permission grant: accessible shared device audio indexed without manual file-by-file importing.
2. Permission denial: app does not crash and explains SAF import option.
3. Add 500 tracks to new playlist; remove/reorder entries; no source files deleted.
4. Close app/reboot: playlists, play queue and last position remain consistent.
5. Connect Bluetooth headphones, lock device, handle phone call/audio focus, unplug headset.
6. Add/remove/rename audio externally: library refresh converges with no duplicates or stale items.
7. Import/export playlist with missing files: missing paths surfaced and surviving tracks remain usable.
8. Scan 10,000+ audio items without UI freezing or expensive repeated full scans.
9. Settings and library support Arabic RTL and English LTR, TalkBack, larger fonts.
10. Build on clean machine, sign, install and perform a real end-to-end playback test.

## Risks and boundaries
- 'All audio on phone' means all media exposed by Android through authorized APIs, not sandbox-private or DRM-protected files.
- Format support depends on codecs installed on device and Android framework; validate MP3, AAC/M4A, WAV, OGG/Opus, FLAC (as supported).
- Audio crossfade and ReplayGain require nontrivial DSP/mixing; optional after baseline and benchmarking.
- Audio content remains local; playlists store references and user metadata only.
- Developer-created application icon and assets must not include third-party trademarks.
- No premature '100% ready' claim before signed APK, device smoke test, and gates passed.

## Persistent execution protocol
- Update docs/PROGRESS.json after each task with task_id, status PLANNED/IN_PROGRESS/PASS/BLOCKED, weight, evidence_path, timestamp, blocker and next_action.
- Percentage = sum(weights of PASS tasks only). BLOCKED contributes zero until retested.
- Attach screenshots/logs/test results to docs/evidence/task-XX/.
- Preserve local working files before any destructive change; save changes to version control with meaningful commits.
- Never publish or overwrite production site without a verified release artifact and complete Gate E.
- Next actionable task: 01 — requirement baseline and build environment verification.

## Official references
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/media/implement/playback-app
- https://developer.android.com/media/media3/session/background-playback
