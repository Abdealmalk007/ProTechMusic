# ProTech Music v0.4.0-beta.2 — Signed Android Preview

**Status: BETA / NOT FINAL (2026-11-22).** The original 25-task product roadmap remains **44% complete (11/25)**, and the feature-closure F phase is **80% complete (12/15)** after evidence-based F12. Production / multi-device gates remain open.

## Changes since v0.3.9-beta.1
- Embedded audio artwork in expanded Now Playing (bounded JPEG/PNG extraction on an IO dispatcher, blank-art fallback).
- The current artwork is also attached to Android MediaSession metadata and playback notification when available.
- Import timestamped `.lrc` lyric files using Android's Storage Access Framework picker; copy into app-private, per-track hash-keyed storage and display the active line.
- Fast, deterministic lyric-time binary search and unit tests for fractions, multi-timestamps, invalid timestamps and indexing.
- Expanded player sheet is scrollable on short viewports, with a bounded viewport height and accessible 48dp media buttons.
- Existing library/source discovery, playlist features, shuffle/repeat, audio effects, signed-app identity and offline-first behavior preserved.

## Evidence
- Offline Gradle Android build and 48/48 JVM tests passed (zero failures).
- Android 15/API35 QA emulator: fresh updated debug APK installation and app launch succeeded; seeded synthetic MP3 files were indexed.
- Embedded cover artwork visible; Android MediaSession supplied artwork metadata for synthetic audio.
- LRC document picker imported 3 lines, the first lyric line rendered correctly, and the 72-byte file was stored privately.
- Foreground notification actions Previous/Pause/Next and Android audio-focus GAIN were observed; automatic queue transitions recorded.
- Full evidence: `docs/QA_F12_ANDROID35_ACCEPTANCE_20261122.md`.

## Caveats
- Preview, not release-certified. Physical Android phone, headset and Bluetooth controls, interrupted phone calls, EN LTR and TalkBack, Android API 26–36 matrix, 10k/50k indexing, memory/battery profiling and release-closure gates are NOT all verified.
- Permissions include only audio readable under the Android scoped-storage model; no hidden/private WhatsApp storage extraction.
- The equalizer, BassBoost and LoudnessEnhancer are device dependent; volume boost is bounded but output quality requires real hardware QA.
- Upgrades from signed beta1 should use the same release signing key. Android debug builds are signed differently and may not install over signed releases, or vice versa. Back up playlists before reinstalling incompatible test builds.
- Compare the SHA-256 of the APK with the included `SHA256SUMS_v040_beta2.txt` before installing.

ALGHAD DIGITAL / ProTech — All Rights Reserved. Third-party components retain their own licenses.
