#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-runtime-evidence
pkg=digital.alghad.protechmusic
runner="$pkg.test/$pkg.RcSmokeInstrumentation"
trap 'adb logcat -d -t 1800 > qa-runtime-evidence/logcat.txt || true' EXIT
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
test "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = 35
if adb shell pm path "$pkg" | grep -q '^package:'; then
  echo 'Unexpected existing installation on disposable runner' >&2
  exit 1
fi
adb install app/build/outputs/apk/debug/app-debug.apk | tee qa-runtime-evidence/install.txt
python3 scripts/qa/nextgen-permission-ui.py | tee qa-runtime-evidence/permission-ui.txt
grep -q PERMISSION_UI_PASS qa-runtime-evidence/permission-ui.txt
adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r "$runner" | tee qa-runtime-evidence/smoke.txt
grep -q RC_SMOKE_PASS qa-runtime-evidence/smoke.txt
! grep -q RC_SMOKE_FAIL qa-runtime-evidence/smoke.txt
adb shell am force-stop "$pkg"
adb shell am start -W -n "$pkg/.MainActivity"
adb shell am instrument -w -r -e mode playback "$runner" | tee qa-runtime-evidence/playback.txt
grep -q RC_PLAYBACK_PASS qa-runtime-evidence/playback.txt
grep -q TRANSPORT_PASS qa-runtime-evidence/playback.txt
grep -q PLAYBACK_MODES_PASS qa-runtime-evidence/playback.txt
grep -q MEDIA_SESSION_PASS qa-runtime-evidence/playback.txt
! grep -q RC_PLAYBACK_FAIL qa-runtime-evidence/playback.txt
echo 'NEXTGEN_RUNTIME_PASS'
