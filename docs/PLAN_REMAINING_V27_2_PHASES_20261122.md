# ProTech Music — Remaining-work execution contract V27
Date: 2026-11-22. Source: `E:\ProTech\ProTechMusic`. Resume from `docs/HANDOFF_V26.json`.
Last public at entry: signed `v0.4.4-beta.6` (code 17). Target: **as few stages as possible: Stage A and Stage B, exactly 15 gates each**. Do not restart finished work.

## Stage A — F14/F15/RC closure (15 gates)
| Gate | Mandatory deliverable | Current audited status |
|---|---|---|
| A01 | Source, tags, handoff, signer provenance reconciled | PASS |
| A02 | Clean emulator boot, ADB, installed app and responsive UI | PASS on earlier V23; V26 signed upgrade separately pending |
| A03 | Real MediaStore 10-song URI/title/search parity | PASS |
| A04 | Real MediaStore 100-song URI/title/search parity | PASS |
| A05 | Real MediaStore 1,000 distinct songs | PASS |
| A06 | App-visible 10,000 real media files, no pending rows; logs/JSON | BLOCKED: QA shell MediaStore IS_PENDING |
| A07 | App-visible 50,000 real media files, timed indexing/ANR evidence | BLOCKED: actual scan not performed |
| A08 | Real scanned Arabic/English search + diacritics, sort parity | PENDING integration evidence; JVM cases previously PASS |
| A09 | SAF/M3U/source permissions, roundtrip/missing/deny tests | PENDING integrated evidence |
| A10 | p50/p95 cold/warm index/search and thresholds, repeated trials | PENDING repeatable measured device logs |
| A11 | Long-running foreground/background CPU/PSS/no ANR | PENDING |
| A12 | Physical phone measured battery/wakelocks | BLOCKED without authorized physical ADB device |
| A13 | Physical Bluetooth/headset/phone call audio focus | BLOCKED without authorized physical ADB device |
| A14 | Android instrumentation, UI widths + 54+ JVM tests | PASS |
| A15 | RC gate zero P0/P1, reproducible signed candidate & upgrade evidence | PENDING; signed beta only so far |

Stage A exit requires 15/15 PASS with raw reproducible evidence. Signed beta release is **not** a substitute.

## Stage B — production qualification and stable release (15 gates)
| Gate | Acceptance requirement | Current status |
|---|---|---|
| B01 | Reconcile original 25 tasks, evidence links, release notes | PENDING |
| B02 | Android API26–28 installation, legacy permissions | PENDING |
| B03 | Android API29–32 SAF, delete consent, MediaStore | PENDING |
| B04 | Android API33–36 runtime permissions and foreground service | PENDING |
| B05 | WhatsApp/Telegram/Downloads/Recordings/voice notes discovery | PENDING |
| B06 | Real device Bluetooth/headset/phone interruptions | BLOCKED until physical testing |
| B07 | Long-session auto-next/previous, queue shuffle/repeat boundaries | PARTIAL: 0→1→2 and repeat/shuffle emulator PASS |
| B08 | Deletion acceptance/cancellation, batches, SAF and no data loss | PENDING |
| B09 | RTL/LTR accessibility, TalkBack, scaling, landscape | PENDING |
| B10 | Real 1k/10k/50k stress p50/p95/PSS/ANR | PENDING |
| B11 | Privacy/security and permission audit, 0 P0/P1 | PENDING |
| B12 | Icon, branding, installer/in-place upgrade preserving data | PENDING |
| B13 | Signed stable APK/AAB, signer/hash and clean build | PENDING |
| B14 | Public stable GitHub Release + bilingual website deployment, live checks | PENDING |
| B15 | Final manual acceptance, all original and release gates PASS | PENDING |

## Execution order and safeguards
1. First independently unblock and verify signed-to-signed Beta5→Beta6 upgrade on a **single disposable emulator**, then build a preview with repeat-mode persistence.
2. In parallel, audit/search/sort tests, SAF/M3U and privacy code, UI instrumentation, release assets and source deployment; retain fail evidence.
3. Attempt real 10k and 50k media and physical measurements only on authorized, non-destructive media environments. Never assert PASS on synthetic 50k records or merely an on-disk file count.
4. Promote only after physical-device A12/A13/B06 and all gates pass; otherwise publish only a clearly labeled signed **Beta/Preview**, with exact blockers, source commit, SHA and site checks.

## Original progress ledger at entry
- Stage A: 6/15 PASS (40%); Stage B: 0/15 PASS (0%).
- F: 13/15 (86.7%); original roadmap: 11/25 (44%); R: 0/15 (0%).
- Do not change any denominator, create a fresh 100%-complete claim, or conflate binary artifact publication with product readiness.
