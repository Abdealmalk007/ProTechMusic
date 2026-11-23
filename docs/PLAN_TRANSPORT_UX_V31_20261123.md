# ProTech Music — V31 focused UX closure (2026-11-23)

Baseline: `v0.4.6-beta.8` (code 19). Candidate: `v0.4.7-beta.9` (code 20).
Scope: **one 15-check improvement stage** for the requested transport and playing-item UI. This is separate from the unfinished production Stage A/B certification.

| ID | Required behavior | Evidence/status |
|---|---|---|
| UX01 | Existing package, signing key, playlists and audio preserved | Source baseline and unchanged package; signing audit pending |
| UX02 | Previous control at least 56dp with semantics | V31 source contract PASS |
| UX03 | Next control at least 56dp with semantics | V31 source contract PASS |
| UX04 | 68dp Play/Pause prominent and centered with previous/next | V31 source contract PASS; emulator screenshot pending |
| UX05 | Track row remains visible at 320dp and in RTL/LTR | Layout width calculation PASS; visual emulator pending |
| UX06 | Remove every album cover from player UI | V31 source contract PASS |
| UX07 | Do not load album image for MediaSession, notification, lockscreen | V31 source contract PASS |
| UX08 | Current track visible with luminous gradient, border, shadow and contrasting title | V31 source contract PASS; runtime visual pending |
| UX09 | Current track remains highlighted when paused | V31 state comparison independent of playing PASS static |
| UX10 | Tapping current song toggles Play/Pause, no queue reset | V31 source contract PASS; runtime pending |
| UX11 | Pause/Resume resumes the same item and position | Previous V29 state checkpoint; V31 runtime pending |
| UX12 | Keep Next/Previous, ±10s, Repeat/Shuffle/MediaSession/notification | V31 source contract PASS; Android regression pending |
| UX13 | Gradle JVM and Debug/Release builds succeed | PENDING completed build log |
| UX14 | Sign original identity, verify v2/v3/version/hash, safe in-place update | PENDING build and signature verification |
| UX15 | Publish GitHub prerelease and AR/EN pages; direct download + live SHA HTTP200 | PENDING release and Pages evidence |

Raw assertions: `docs/evidence/v31/source_contract.json` (15/15 source checks only).
Build logs: `docs/build_v047_beta9_all.log` (must contain BUILD SUCCESSFUL before signing).
Site and release scripts: `E:\ProTech\Releases\ProTechMusic\*_v047*` (not a release until verified).

Unfinished broader gates: A06/A07 real visible 10k/50k MediaStore, A12/A13 and B06 physical hardware, API26–36, sustained battery, headset/calls and accessibility. These may not be counted as passed through static source tests. Never call this beta production Stable/Final.
