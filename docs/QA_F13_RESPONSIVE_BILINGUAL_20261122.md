# ProTech Music F13 — Responsive layout & bilingual UI acceptance
Date: 2026-11-22
Status: PASS for F13 emulator UI matrix. **NOT a final/stable release certificate.**

## Implemented
- Arabic RTL remains the default language; a visible AR/EN toggle switches to English LTR and persists its choice in app-local SharedPreferences.
- Main screens and navigation labels use the offline UiLanguage dictionary; non-translated media filenames/tags remain unchanged.
- NavigationBar's compact/large-font display uses icons with TalkBack content descriptions instead of clipped tab labels.
- Landscape layout uses a scrollable side navigation instead of a tall bottom bar.
- Landscape audio-library search/chips are collapsible with the Search and filters control to preserve room for the audio list at 320dp height.
- Existing playback, media permissions, local lyrics and artwork remain intact.

## Actual Android QA evidence
Test host DESKTOP-PM75RQM, Android API35 emulator ProTechMusicQA35_211126, test-only media, debug signed locally (not intended for public release).
- 320dp portrait at 160% font, Arabic RTL, seeded tracks visible; compact icon navigation has no text clipped: `docs/evidence/f13/f13_v041_320dp_ar_font160.png`
- 320dp portrait at 160% font, English LTR: full search and track library, no truncation in bottom navigation because text labels are hidden in compact layout: `docs/evidence/f13/f13_v041_320dp_en_ready.png`
- Toggle persisted: SharedPreferences `ui_language.xml` showed english=true after English toggle, english=false after Arabic toggle; orientation/layout changed immediately and persisted after activity restart.
- Landscape at 640dp x 320dp with font 160%, English LTR: side navigation, audio tracks visible with filters collapsed: `docs/evidence/f13/f13_v041_landscape_en_font160.png`
- Opening the Search and filters control displayed the chips and search input: `docs/evidence/f13/f13_v041_landscape_filters_expanded.png`
- 411dp portrait at normal font with three-button Android navigation enabled: content, player bar and bottom navigation clear above OS system navigation buttons: `docs/evidence/f13/f13_v041_411dp_ar_threebutton_ready.png`
- 411dp English portrait at normal font was inspected on the prior incremental debug build, see `docs/evidence/f13/f13_411dp_en_ready.png`. Layout changes since that screenshot primarily affected compact and landscape modes.
- Final `assembleDebug` plus `testDebugUnitTest`: BUILD SUCCESSFUL; 50/50 unit tests passed, 0 failures. Logs: `docs/F13_v041_debug_final.log` (untracked build log).
- Tested app PID remained running; no AndroidRuntime FATAL EXCEPTION observed in inspected logcat window.

## Remaining limitations
- Broad physical-device/TalkBack testing, phone-call/Bluetooth interruption reliability, Android 8–16 matrix, F14 1k/10k/50k benchmarks, F15 RC and R01-R15 release gate remain open.
- This QA proves main-layout and navigation scenarios on API35 only, not comprehensive translation of all error toasts/system strings or all OEM font/rendering variations.
- No personal user audio files were deleted or modified.

## Progress
- Feature closure phase F: 13/15 verified = 86.7%. Next F14.
- Original 25 weighted product tasks: 11/25 = 44% (unchanged until corresponding original gates pass).
- Release gate R: 0/15 = 0%; current public channel remains Beta/Preview.
