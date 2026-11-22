# ProTech Music — Remaining Work / Minimal Two-Stage Plan (V23)
**Date:** 2026-11-22 · **Project:** `E:\ProTech\ProTechMusic` · **Source baseline:** `001b489` · **Published signed beta:** `v0.4.3-beta.5`

## Execution contract
- Exactly **two sequential stages**, **15 auditable tasks per stage** (30 checks), organized by **RC qualification** then **production release qualification**.
- Complete tasks in dependency order where necessary; continue independent tasks when another is BLOCKED. Never use aspirational progress: each PASS requires a command/test result, raw evidence file and commit or inspection trail; treat UNVERIFIED as BLOCKED, not PASS.
- Stage percent = PASS count / 15 × 100. **Do not conflate** with original weighted roadmap (11/25 = 44%), feature closure F (13/15 = 86.7%), production phase R (0/15 = 0%). Only adjust those if their separate gates are truly satisfied.
- Never delete personal audio, wipe user data, publish Debug APK, replace signing key, or claim simulated battery readings are physical. Test files are created solely on a disposable emulator in isolated `Music/ProTechMusicQA_*/` folders. Real devices require explicit authorized availability.
- Resume from `docs/HANDOFF_V23.json` and `docs/REMAINING_2X15_STATUS_V23.json`, never restart completed work. Keep `BLOCKED` reasons precise.

## Stage A — Finish F14/F15 and certify an RC (15 tasks)
| ID | Execution task | Mandatory PASS evidence |
|---|---|---|
| A01 | Confirm Git branch, signed Beta5 baseline and previously recorded F14/F15 progress | Git ref, handoff and release-gate reconciliation |
| A02 | Stabilize API35 emulator, ADB and non-streamed Debug installation; no destructive reset | Cold boot, responsive ADB, actual installed package and launch logs |
| A03 | Run actual MediaStore WAV fixture at **10** unique songs, title and URI query parity | On-device `f14_real_10.json`; count/search and crash-free logs |
| A04 | Run true-media **100** files in fresh isolated folder | Valid JSON with 100 distinct MediaStore tracks |
| A05 | Run true-media **1,000** files and compare normalized index vs source | Valid 1k scan/index metrics, no duplicate contamination |
| A06 | Test actual MediaStore **10,000** songs (prefer disposable device; no user music) | Verified real distinct files, bounded scan/index evidence |
| A07 | Test actual MediaStore **50,000** songs | Verified provider count, start/warm latency, no ANR |
| A08 | Repeat Arabic/English/diacritics search and sort against real scanned files and playlists | Results parity tests, zero duplicates, queue persistence |
| A09 | SAF/M3U and source discovery integration regression, permissions and error handling | Roundtrip and storage permission evidence |
| A10 | Repeated cold/warm search, prepare and provider scans; record p50/p95 and latency criteria | Repeatable JSON/CSV and accepted thresholds |
| A11 | Profile long-running foreground/background PSS, CPU and lifecycle, no leak or ANR | Continuous memory and CPU device evidence |
| A12 | Measure battery on actual Android hardware; foreground/background power and wakelocks | Physical device controlled experiment, never emulator estimate |
| A13 | Validate headset/Bluetooth/media buttons, phone interruption and audio focus | Authorized physical device logs/screens and no P0/P1 |
| A14 | Build and run Android instrumentation smoke + UI tests and JVM suite across layouts | Instrumentation execution results, 54+ JVM PASS, screenshots |
| A15 | RC clean build, signing, upgrade, release manifest audit and triage to zero P0/P1 | Signed RC candidate, version/SHA, RC checklist fully PASS |

**Stage A exit:** only then may F14 and F15 be marked PASS and move to Stage B as a release candidate. While dependencies are blocked, independently improve tests and do not promote.

## Stage B — Production validation and final public release (15 tasks)
| ID | Execution task | Mandatory PASS evidence |
|---|---|---|
| B01 | Final roadmap reconciliation, feature-by-feature evidence and RC changelog | Audited 25-task ledger + release notes |
| B02 | Android API26–28 install/permissions/legacy storage | Emulator API matrix and clean app launches |
| B03 | Android API29–32 scoped storage, MediaStore delete/SAF | Tested create/delete/deny and no lost user data |
| B04 | Android API33–36 foreground service and permission matrix | Permission and lifecycle QA logs |
| B05 | WhatsApp, Telegram, Downloads, voice notes, Recordings and Music discovery | Per-source real hardware/fixture coverage |
| B06 | Bluetooth/headset/call/playback focus reliability on devices | Physical evidence and fix verification |
| B07 | Long-session playback, queue next/previous, repeat/shuffle boundaries | Deterministic long-run tests with MediaSession data |
| B08 | Safe deletion consent, cancellation, batch, missing files and SAF | App + OS consent; only disposable QA fixtures |
| B09 | Arabic RTL, English LTR, accessibility/TalkBack, large fonts, narrow landscape UI | Screenshots, contrast, navigation and screen-reader QA |
| B10 | 1k/10k/50k sustained stress, search latency and memory regression | Repeatable performance report and thresholds |
| B11 | Privacy/security audit, minimal permissions, stored data and error handling | Audit artifact, zero blocking issues |
| B12 | Launcher icon/adaptive icon, installer, upgrade, copy and branding | Installed screenshots + version/package consistency |
| B13 | Clean signed stable APK/AAB, fixed key, SHA256 and reproducible build | Verified signatures/cert + clean CI/local build |
| B14 | Push final GitHub Release and update alghad.digital AR/EN live links | Real release assets, site deployment PASS, HTTP200/version/hash |
| B15 | Final end-to-end acceptance + 25-task reconciliation and 0 P0/P1 | Signed production release and evidence-gated 100% |

**Stage B exit:** stable public build only when B01–B15 PASS and all original 25 tasks have real evidence. If a hardware prerequisite is absent, stop with BLOCKED; Beta remains the last public build.

## Existing, verified baseline
- Source has F13/F14 earlier QA, 54/54 JVM test PASS, signed Beta5 on GitHub and the bilingual site.
- Previous Android synthetic 1k/10k/50k tests are **not** a substitute for actual 50k files.
- Real-media V21 10-file scan reached expected count but title assertion FAILED; V22 title/URI fix compiled, runtime BLOCKED by unresponsive emulator/ADB. No valid V22 JSON, no physical battery data.
- Physical ADB device: **none connected** at start of this plan. Stage A cannot be certified yet.
