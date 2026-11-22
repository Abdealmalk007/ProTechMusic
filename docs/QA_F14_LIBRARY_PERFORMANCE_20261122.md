# ProTech Music — F14 large-library performance QA (partial gate)

Date: 2026-11-22. **Status: IN_PROGRESS / NOT GATE PASS.**
Main test hardware: DESKTOP-PM75RQM, JDK 17, Kotlin JVM unit tests. Android synthetic benchmark target: Android API 35 emulator.

## Implemented changes
- `TrackSearchEngine.PreparedLibrary` precomputes normalized text, titles, artists, sources per library snapshot. `MainActivity.refresh()` builds index on `Dispatchers.Default`, outside the main/UI thread.
- Production `AudioCursorReader.appendTo` reads provider rows with URI-keyed deduplication and nonpositive-size exclusion. Same parser used by Android debug synthetic `MatrixCursor` stress harness; debug activity never enters release manifest.
- Tests compare new indexed search with original stateless logic for every sort order, Arabic, filtering, ad-hoc tracks, and stable original playlists.

## Actual JVM measurements (synthetic objects; single trial per size)
| Records | Index build ms | Indexed query ms | Old query ms | Indexed sort all ms |
| --- | ---: | ---: | ---: | ---: |
| 1,000 | 15.716 | 1.003 | 8.106 | 2.769 |
| 10,000 | 139.835 | 9.435 | 63.247 | 24.911 |
| 50,000 | 225.610 | 21.966 | 131.189 | 79.426 |

Actual offline Gradle `assembleDebug` and `testDebugUnitTest`: **53 tests / 0 failures / 0 errors**. Raw CSV `docs/evidence/f14/jvm_benchmark.csv` and JSON summary. These are **not** end-to-end MediaStore measurements. Search times include a single invocation, not a latency percentile. Heap deltas contain GC and allocation noise; do not infer steady-state process RSS from them.

## Android stress status
- Debug-only `F14BenchmarkActivity` is permission-gated by `android.permission.DUMP`, and creates no real audio files. It generates 1,000/10,000/50,000 synthetic MatrixCursor metadata rows (plus a duplicate URI and zero-byte row), runs the production parser, prepared index, five queries and sorting, and captures app `Debug.getPss()`, heap and CPU.
- First 1k attempt discovered a flawed test expectation: searching `Track 999` matched more than one record because search legitimately covers multiple fields and matches token substrings. This was corrected to use a uniquely named sentinel `BenchmarkNeedle_${n}`. The test harness uses distinct fixture labels and explicitly checks one match. The code is debug-only and is not a product-search bug.
- **Android API35 synthetic MatrixCursor benchmarks PASSED at 1k, 10k and 50k**: 1,002/10,002/50,002 supplied rows include a deliberately duplicate ID and a zero-size media row. Parsed unique count exactly 1,000/10,000/50,000, all five prepared-search queries returned one uniquely named sentinel track per test, and no AndroidRuntime FATAL exceptions appeared in checked logcat. Raw JSON and logcat: `docs/evidence/f14/android_api35_*.json`, `f14_android_runtime_errors.txt`.
- **Actual Android 15 timings (ms)**, including cold JVM/ART warmup effects and emulator variability:

| Synthetic rows (unique) | Cursor parse | Prepared index build | Indexed search five runs | Sort all | End PSS |
| --- | ---: | ---: | --- | ---: | ---: |
| 1,000 | 385.4 | 1,918.8 | 15.5, 8.4, 9.1, 8.2, 10.0 | 62.9 | 76,513 KB |
| 10,000 | 1,050.1 | 10,004.1 | 100.1, 70.0, 164.4, 58.0, 64.6 | 299.8 | 101,543 KB |
| 50,000 | 2,711.8 | 21,896.3 | 24.9, 26.5, 25.9, 26.2, 25.7 | 117.1 | 172,172 KB |

- Memory PSS starts 50,541 KB / 68,098 KB / 67,876 KB for the three runs. These are separate process snapshots, not a controlled peak-memory profile. Benchmark was sequential on an emulator with 9 real test-fixture MediaStore audio records, so this **does not** establish real-device 50k provider scan throughput or battery drain.
- **Performance concern:** building the 50k index took ~21.9 sec on the low-spec API35 emulator even off the UI thread; while browsing remains responsive after the index builds, full-library readiness and loader UX need further profiling/optimization. Therefore **F14 remains IN_PROGRESS**.
- Device battery discharge, true background wakeups and 1k/10k/50k **actual distinct audio files** **NOT MEASURED**. Android emulator power statistics cannot certify physical battery consumption. Physical-device measurement and true provider integration remain mandatory F14 gates.

## Progress contract
- Official original roadmap **44%** (11/25) unchanged.
- Feature F milestone **86.7%** (13/15 PASS). F14 **IN_PROGRESS**, F15 not passed.
- Release R **0/15 PASS**. Public artifacts may be **signed Beta Preview** only, not final/stable.
