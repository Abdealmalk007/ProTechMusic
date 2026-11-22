# ProTech Music 0.4.4-beta.6 — Compact UI & Playback QA Preview

**Channel: PRE-RELEASE BETA (not a final/stable version).** Signed Android APK upgrade from prior official Beta5 without replacing the existing signing certificate.

## Changes
- Compact screen layout (<360 dp): fully visible **All** and **More** controls replace the clipped horizontal quick-action strip. The More menu keeps Import, Queue, Recently Added, Sources, Folders, Artists, Albums and Select actions available.
- Standard portrait and landscape layouts retain their existing navigation.
- Debug-only automated validation expanded. Three actual temporary WAV files were played in sequence on Android 15; the recorded player transitions were **0 → 1 → 2**, with repeat OFF/ONE/ALL and shuffle ON/OFF controls verified.
- Real `MediaStore` scans at **10, 100 and 1,000** files passed on Android 15; **54/54 JVM tests** passed.

## Important limits
- Real 10k/50k *app-visible* audio-file scanning and extended memory/ANR benchmarks are **not certified**. The QA shell's larger fixtures remained `IS_PENDING`, so the app legitimately could not see them. Raw evidence and issues are in `docs/QA_V25_REAL_MEDIA_PLAYBACK_20261122.md`.
- Battery drain, long-run background behavior, Bluetooth, wired headset and phone interruption tests need an authorized physical device; none was connected during this test run.
- This beta should not be labeled stable or final until all F14/F15 and production release gates pass.

## Safety
- Debug-only QA generators/activities are not packaged into the signed release APK.
- Do not uninstall the previous signed beta when upgrading. Existing app data should be retained on in-place installs.
- ALGHAD DIGITAL / ProTech. All Rights Reserved. Third-party dependencies retain their licenses.
