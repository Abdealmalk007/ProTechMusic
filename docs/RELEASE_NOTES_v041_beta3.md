# ProTech Music v0.4.1-beta.3 — signed Android preview

**Beta / Preview — not final or production-certified.** Original 25-task roadmap: **44%** complete (11/25 evidence-backed tasks). Feature-completion Phase F: **86.7%** (13/15). Release gate R remains **0/15**.

## Changes since v0.4.0-beta.2
- Toggle between Arabic RTL (default) and English LTR, with selected language persisted between app launches.
- Offline English wording across main library, search, playlists, playback controls, media effects and navigation.
- Responsive 320dp portrait, 411dp portrait and short landscape screen layouts.
- Compact / large-font navigation switches to icon-focused controls with accessible descriptions rather than truncated labels.
- Landscape side navigation with a collapsible Search and filters panel for more room to browse and play music.
- All prior locally indexed audio, queue, playlists, cover art, imported synchronized LRC lyrics, notifications and audio effects retained.

## Verification
- Android 15 (API35) emulator: 320dp RTL and LTR at 160% font, landscape at 160% font with search collapsed/expanded, 411dp portrait with Android three-button navigation. Evidence stored under `docs/evidence/f13/`.
- 50/50 JVM unit tests passed, including locale/persistence helper regression checks; signed APK signing certificate and SHA-256 should be verified using attached checksum file.
- Evidence: `docs/QA_F13_RESPONSIVE_BILINGUAL_20261122.md`.

## Limitations and upgrade notice
- This preview is not final. Comprehensive real-phone/TalkBack verification, Android 8–16 testing, Bluetooth/headset and interruption behavior, 10k–50k indexing, long-run battery profiling, RC and R01–R15 release gates remain pending.
- Some ancillary error messages and OEM/system UI text may remain Arabic; the documented F13 checks cover primary views and navigation.
- When upgrading from the previous signed Beta, Android uses the same original project signing certificate; do not uninstall an existing signed app unless necessary, to preserve private playlists and configuration. Debug APKs use a different signing key.
- The app only accesses Android-permitted local media. No DRM/private-app storage bypass.

ALGHAD DIGITAL / ProTech — All Rights Reserved. Third-party dependencies retain their own licenses.
