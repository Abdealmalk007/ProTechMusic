# ProTech Music — V29 navigation and playback UX upgrade (15 checks in one stage)

Date: 2026-11-22. Baseline: signed v0.4.5-beta.7 (versionCode 18). Target: v0.4.6-beta.8 (versionCode 19).

| # | Improvement / acceptance | Status |
|---|---|---|
| 01 | Preserve source, current signer, repository, app ID and audio data | IMPLEMENTED, release verification pending |
| 02 | Previous transport button at least 56dp with accessible description | IMPLEMENTED, visual test pending |
| 03 | Next transport button at least 56dp with accessible description | IMPLEMENTED, visual test pending |
| 04 | Main play/pause button 68dp, large glyph, distinct high-contrast style | IMPLEMENTED, visual test pending |
| 05 | Show running/paused mini-player state clearly in Arabic and English | IMPLEMENTED, visual test pending |
| 06 | Reliable pause/resume using existing ExoPlayer, no queue reset | IMPLEMENTED, runtime test pending |
| 07 | 10-second rewind and forward controls on expanded player | IMPLEMENTED, runtime test pending |
| 08 | Clamp seek within 0..duration and sync MediaSession | IMPLEMENTED, runtime test pending |
| 09 | Handle notification transport action when service process was recreated | IMPLEMENTED, runtime test pending |
| 10 | Save current position/index periodically without rewriting large queues | IMPLEMENTED, runtime test pending |
| 11 | Preserve existing automatic next, repeat, shuffle and sleep timer | PREVIOUSLY VERIFIED in V25, V29 regression pending |
| 12 | Validate 320dp compact, RTL/LTR and expanded player with screenshots | PENDING |
| 13 | Run full JVM and Android build/instrumentation QA | PENDING |
| 14 | Generate stable-key signed APK, match SHA-256, versionCode, v2/v3 | PENDING |
| 15 | Publish versioned prerelease with hash on GitHub and AR/EN website, verify live | PENDING |

The 15 items are this focused improvement stage, NOT the broader production certification. Existing unfinished gates A06/A07/A12/A13 and Stage B remain unapproved pending physical-device and large-MediaStore evidence. Never delete user audio. Do not relabel beta as final stable.
