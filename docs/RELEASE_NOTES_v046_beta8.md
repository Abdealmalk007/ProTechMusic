# ProTech Music v0.4.6-beta.8 — Larger playback controls and reliable resume

**Channel:** signed BETA / PRE-RELEASE, not certified stable.

## User-facing improvements
- Previous/next are 56dp accessible buttons; central play/pause is a prominent 68dp button with explicit Arabic and English accessibility labels.
- Mini-player shows whether the track is playing or paused, and clearly indicates how to resume.
- Expanded player adds 10-second rewind/forward controls, beside existing scrub slider, speed, shuffle, repeat, lyrics and sleep timer.
- MediaSession and notification transport controls are retained; service now restores the persisted queue before handling a notification action after process recreation.
- Played song position is checkpointed every 10 seconds with inexpensive preference writes, rather than serializing the full queue on each tick.
- Seeking is clamped to valid track duration and playback state reflected to MediaSession.

## Compatibility and safety
- Existing app ID digital.alghad.protechmusic; versionCode 19.
- Reuse the SAME existing production signing key as beta7. No user audio is modified.
- Playback may pause on audio-focus loss and headphone disconnect through existing ExoPlayer audio attributes and noisy-output handling.

## Outstanding production release gates
The physical-device battery, Bluetooth, wired-headset, telephony interruption, API26–36 full compatibility and app-visible 10k/50k MediaStore tests remain unapproved. This is a usable beta preview, not a final stable certificate.

ALGHAD DIGITAL · ProTech · All Rights Reserved.
