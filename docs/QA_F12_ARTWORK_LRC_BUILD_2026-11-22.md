# F12 partial QA — artwork and local LRC lyrics (2026-11-22)
Status: IN_PROGRESS (not a release gate PASS).

## Implementation
- Embedded cover art extracted from a user-accessible track URI using MediaMetadataRetriever off the UI thread.
- Artwork bytes capped at 3 MiB; bitmap downsampled to around 400 px, null fallback with a note icon.
- LrcLyrics parser supports multiple timestamps per line, 1–3 fractional digits, sorted timed lines, and O(log n) active lyric selection.
- LRC import uses Android OpenDocument SAF, limits input to 512 KiB, stores a private per-track SHA-256-keyed file, and loads on IO dispatcher. Reopening player shows stored lyrics.
- The feature is local-only; no external lyrics provider or network permission added.

## Verified automated evidence
- Build command: offline Gradle 8.11.1 / JDK17 / SDK35 :app:assembleDebug :app:testDebugUnitTest
- Log: docs/build_f12_20261122.log, BUILD SUCCESSFUL, exit 0.
- 48 unit tests passed and 0 failed from app/build/test-results/testDebugUnitTest/*.xml.
- New LrcLyricsTest includes timestamp fractions, duplicate timestamps, invalid seconds, active-line binary search and metadata filtering.
- All prior core tests still pass.

## Not verified, cannot mark PASS
- Android 35 API emulator installation and MainActivity launch passed a limited smoke test, but visual artwork decode, LRC OpenDocument import, on-screen synchronization and audio-session transitions still need real instrumentation evidence.
- Runtime smoke evidence: AVD ProTechMusicQA35_211126 (API35), ADB serial emulator-5556, `adb install -r` -> Success / exit 0, `am start` -> exit 0, PID 2304, dumpsys shows visible MainActivity, no app FATAL EXCEPTION in inspected AndroidRuntime logcat window.
- Screenshot saved locally at qa/f12_launch_api35.png (ignored by Git). It shows the app UI with zero audio tracks and an Android System UI `isn't responding` warning; the emulator environment is unstable for final UI, cover-art, lyric or audio-focus acceptance testing.
- No real Android phone/Bluetooth/headset test; no background audio-focus regression proof or 1k/10k/50k performance metrics.
- New source is not included in the existing signed v0.3.9-beta.1 GitHub release.
- The signed beta available on the site predates this commit. Never link the debug build as the final installable release.
- F12 remains IN_PROGRESS; official weighted roadmap stays at 44% (11/25). Phase F 11/15 (73.3%), R 0/15.
