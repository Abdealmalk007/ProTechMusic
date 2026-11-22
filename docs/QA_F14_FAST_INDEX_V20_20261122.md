# ProTech Music — F14 V20 indexed search optimization QA
Date: 2026-11-22. Status: PARTIAL PASS; F14 NOT CLOSED.

## Code changes
- `TrackSearchEngine.normalize` uses a semantics-preserving printable-ASCII fast path with collapsed spaces, no per-field NFKD/Regex.
- Other Unicode inputs, including Arabic and accented characters, retain prior `Normalizer.NFKD`/diacritic rules.
- Existing prepared-library semantics, URI lookup, playlist order, filtering and source handling unchanged.
- `PreparedTrackSearchTest` gains 2,000 deterministic random printable-ASCII cases and representative Arabic, diacritic, fullwidth and control cases against a copy of the original normalizer.

## Build & tests
- Gradle :app:testDebugUnitTest :app:assembleDebug: BUILD SUCCESSFUL.
- JVM unit tests: **54 passed, 0 failures, 0 errors**.
- Android 15 / API35 emulator synthetic `MatrixCursor` stress: all 1k, 10k, 50k unique records PASS, each with a zero-byte and duplicate-ID extra row, and search correctness checks.
- No checked AndroidRuntime FATAL in `docs/evidence/f14_v20/f14_android_runtime_errors.txt`.

## Actual measured durations — synthetic rows
| Unique records | Beta4 index build ms | V20 index build ms | Beta4 cursor parse ms | V20 cursor parse ms | V20 PSS end KB |
|---:|---:|---:|---:|---:|---:|
| 1,000 | 1918.8 | 3328.3 | 385.4 | 2614.4 | 61,015 |
| 10,000 | 10004.1 | 1180.9 | 1050.1 | 2156.4 | 76,982 |
| 50,000 | 21896.3 | 5070.1 | 2711.8 | 15666.8 | 108,681 |

The index construction improved significantly at the two larger sizes, but the 1k run and unchanged cursor parser were slower in the second run. These are separate emulator sessions, subject to cold starts, ART/JIT warm-up, OS contention, filesystem caches and thermal/host performance. They are *not* controlled A/B speedups or reproducible percentile thresholds. End PSS is not peak memory. A/B and physical-device profiling remain warranted.

Raw data:
- Previous: `docs/evidence/f14/android_api35_{1000,10000,50000}.json`
- Current: `docs/evidence/f14_v20/android_api35_{1000,10000,50000}.json`
- JVM report: `app/build/test-results/testDebugUnitTest/` and `docs/F14_V20_validation_20261122.log`.

## Remaining release gates
- F14: 1k/10k/50k **real distinct audio files scanned by MediaStore** and calibrated physical-device battery/background profile were **not performed**; no physical ADB device was connected.
- 50k responsive UI / ANR on low-end physical device and repeated warm/cold percentiles need measurement.
- F15 RC gate and R01-R15 production tasks remain open.
- Official progress preserved: F **13/15=86.7%**, original roadmap **11/25=44%**, R **0/15=0%**. Beta preview is not stable or final.
