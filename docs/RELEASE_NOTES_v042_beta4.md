# ProTech Music v0.4.2-beta.4 — Optimized Audio Library Preview

**Signed Beta/Preview only, NOT production-ready or a final release.**

## What's new
- Cached, per-library search index: normalized title, artist, source and searchable fields are computed once per audio-library snapshot and reused across searches and sort operations.
- Prepared index generation is dispatched away from the main UI thread; avoids re-normalizing tens of thousands of titles for every typed query.
- MediaStore row decoding extracted into a dedicated, shared production parser with URI deduplication, zero-size track filtering and source classification preserved.
- A debug-only Android benchmark activity uses synthetic cursors of 1,000, 10,000 and 50,000 rows. It is not included in public release APK builds.

## Verified automated tests
- 53/53 Kotlin JVM unit tests passed, including parity against original search output for all sort modes and filtering subsets, empty libraries and snapshot stability.
- One representative JVM synthetic-library trial per size (milliseconds, lower is faster):

| Records | Legacy search | Prepared search | Index construction |
| --- | ---: | ---: | ---: |
| 1,000 | 8.106 | 1.003 | 15.716 |
| 10,000 | 63.247 | 9.435 | 139.835 |
| 50,000 | 131.189 | 21.966 | 225.610 |

Raw measurements in `docs/evidence/f14/jvm_benchmark.csv`. Device hardware, JVM warmup, GC and data composition affect timing. **This is NOT an end-to-end 50,000-file MediaStore or battery test.**

## Release status
- Original 25-task product roadmap: 11/25 verified = 44%.
- Feature closure phase F: 13/15 verified = 86.7%, with F14 **IN_PROGRESS** and F15 not yet certified.
- Production release phase R: 0/15 = 0%.
- F14 full real-media scanning at 1k/10k/50k, long-running foreground/background memory, real-phone power draw and physical-device Bluetooth/call QA remain pending.
- Android 8+; same package `digital.alghad.protechmusic`; v0.4.2-beta.4 versionCode 15. Use the signed APK asset; do not install the debug benchmark APK as a user build.
- An update from the previous *signed* Beta should retain app data if signed using the same original key. Never uninstall solely to upgrade.

ALGHAD DIGITAL / ProTech — All Rights Reserved; third-party dependencies retain their own licenses.
