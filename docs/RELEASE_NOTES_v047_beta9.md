# ProTech Music v0.4.7-beta.9 — Centered transport, active-track glow, no album artwork

**Channel:** BETA PREVIEW — not a production/stable certificate.
**Date:** 2026-11-23 · **Package:** `digital.alghad.protechmusic` · **versionCode:** 20.

## Improvements

- Centered primary transport row with 56dp Previous/Next and prominent 68dp Play/Pause; playback actions remain visible on compact screens.
- Currently selected audio item has a contrasting turquoise title, graduated luminous background, thin bright border, and softly glowing shadow. The indication stays visible while paused.
- Tapping the currently active row or its play tile toggles Pause/Resume without recreating the playback queue or restarting at zero. Tapping a different track starts that track.
- Remove album cover artwork from expanded player **and** MediaSession metadata/notifications/lock screen. Preserve audio browsing, artist names and LRC lyrics.
- Previous/next, ±10-second jumps, progress scrubber, repeat, shuffle, speed, notification media transport, and periodic saved-position checkpoints remain supported.
- Existing package ID, release signing identity and user music/data preserved; no user audio deleted.

## Release gates and restrictions

- V31 source-contract test is 15/15 PASS. Gradle/JVM tests, signature, APK SHA and public release/site checks must be verified separately.
- Full 10k/50k real app-visible MediaStore, Android API26–36 compatibility matrix and physical Bluetooth/battery/call interruption tests are outstanding. Do not label this release Stable, RC certified or Final.
- Keep ALGHAD DIGITAL / ProTech branding; All Rights Reserved.
