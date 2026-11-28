#!/usr/bin/env bash
set -euo pipefail
count="${1:?fixture size required}"
case "$count" in 1000|10000|50000) ;; *) exit 2;; esac
pkg=digital.alghad.protechmusic
out=qa-scale-evidence
mkdir -p "$out"
trap 'adb shell run-as "$pkg" cat "files/f14_real_${count}_diagnostics.json" > "$out/diagnostics.json" 2>/dev/null || true; adb logcat -d -t 1800 > "$out/logcat.txt" || true' EXIT
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
test "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = 35
test "$(adb devices | grep -c $'\tdevice$')" = 1
adb shell getprop ro.product.model | grep -Eq 'sdk_|Emulator'
if adb shell pm list packages "$pkg" | grep -qx "package:$pkg"; then
  echo 'Refusing non-fresh installation' >&2; exit 1
fi
adb install app-debug.apk | tee "$out/install.txt"
adb shell pm grant "$pkg" android.permission.READ_MEDIA_AUDIO
adb shell am start -W -n "$pkg/.F14RealMediaQaActivity" --ei count "$count" --ez scanOnly false
base="f14_real_$count"
deadline=$((SECONDS+10200))
while (( SECONDS < deadline )); do
  if adb shell run-as "$pkg" cat "files/$base.error" > "$out/error.tmp" 2>/dev/null; then
    mv "$out/error.tmp" "$out/error.txt"; cat "$out/error.txt"; exit 1
  fi
  if adb shell run-as "$pkg" cat "files/$base.json" > "$out/report.tmp" 2>/dev/null; then
    if python3 -m json.tool "$out/report.tmp" >/dev/null 2>&1; then
      mv "$out/report.tmp" "$out/report.json"
      python3 - "$count" "$out/report.json" <<'PY'
import json,sys,math
n=int(sys.argv[1])
r=json.load(open(sys.argv[2]))
assert r["mode"]=="ACTUAL_MEDIASTORE_INSERTED_WAV_FILES_API35_EMULATOR",r
assert r["count"]==r["verified_qa_files"]==r["newly_inserted"]==n,r
assert r["real_scanned_total"]>=n and not r["scan_only"],r
for k in ("real_scan_ms_last","index_build_ms","search_ms","pss_end_kb"):
    assert math.isfinite(r[k]) and r[k]>=0,(k,r)
print("REAL_MEDIASTORE_SCALE_PASS",n,json.dumps(r))
PY
      exit 0
    fi
  fi
  sleep 15
done
echo "Fixture timed out without verified report" >&2
exit 1
