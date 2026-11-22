# ProTech Music v0.4.3-beta.5 — Large-Library Indexing Preview

**Pre-release beta only.** This signed Android APK uses the same signing certificate as prior beta builds and is intended as an in-place upgrade.

## Improvements
- Faster index preparation for large ASCII-heavy audio libraries; a printable ASCII fast-path avoids repeated `NFKD` normalization and temporary regular expressions.
- Arabic text, diacritics, Unicode matching, filtering, sort modes, playlists and media playback preserve the prior behavior.
- Automated normalization compatibility tests expanded, covering Arabic/Unicode and 2,000 deterministic ASCII cases.

## Actual tests and limitations
- Kotlin/JVM: 54/54 tests passed, 0 failures.
- Android API35 synthetic `MediaStore`-shaped cursor: 1k, 10k and 50k rows verified; index-build measured at **1.18s for 10k** and **5.07s for 50k** in one emulator run. Previous beta4 isolated run was 10.00s and 21.90s respectively. Cold-start results varied and these separate runs are not controlled A/B performance proof.
- This is **not** validation of 50,000 actual media files on a real phone or calibrated physical battery drain. Device-level F14 remains **IN_PROGRESS**.

## Progress
Original roadmap **44%** (11/25); feature-phase F **86.7%** (13/15 PASS); release-phase R **0%** (0/15 PASS).
See `docs/QA_F14_FAST_INDEX_V20_20261122.md` in the repository for raw evidence and limitations.

Do not uninstall the old signed beta solely to upgrade; use the signed release APK. Debug-only benchmark instrumentation is excluded from release builds.

ALGHAD DIGITAL — ProTech. All Rights Reserved. Third-party dependencies retain their applicable licenses.
