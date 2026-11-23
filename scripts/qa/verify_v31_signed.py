"""ProTech Music Beta 9 signed APK verification. Python 3.14 stdlib."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone

root = Path(r"E:\ProTech\ProTechMusic")
sdk = Path(r"E:\ProTech\AndroidToolchain\sdk\build-tools\35.0.0")
apk = Path(r"E:\ProTech\Releases\ProTechMusic\ProTechMusic_v0.4.7_beta9_SIGNED.apk")
sha = apk.with_name("SHA256SUMS_v047_beta9.txt")
checks = {}
details = {}
if not apk.is_file() or not sha.is_file():
    raise SystemExit("Missing signed APK/checksum")
digest = hashlib.sha256(apk.read_bytes()).hexdigest().upper()
expected = sha.read_text(encoding="utf-8").split()[0].upper()
checks["sha256_matches_checksum"] = digest == expected
checks["apk_sensible_size"] = apk.stat().st_size > 1_000_000
def run(command):
    proc = subprocess.run(command, capture_output=True, text=True, errors="replace", timeout=60)
    return proc.returncode, (proc.stdout or "") + (proc.stderr or "")
code, info = run([str(sdk/"aapt.exe"),"dump","badging",str(apk)])
checks["aapt_success"] = code == 0
checks["package_version"] = ("name='digital.alghad.protechmusic'" in info and
                             "versionCode='20'" in info and
                             "versionName='0.4.7-beta.9'" in info)
details["package_line"] = next((line for line in info.splitlines() if line.startswith("package:")), "")
code, signing = run([str(sdk/"apksigner.bat"),"verify","--verbose","--print-certs",str(apk)])
checks["apksigner_success"] = code == 0
checks["v2_scheme"] = bool(re.search(r"Verified using v2 scheme.*:\s*true", signing))
checks["v3_scheme"] = bool(re.search(r"Verified using v3 scheme.*:\s*true", signing))
signer = "62889112b8a9eaaf531b508a0d3eb965671ef6a757492cfba3682d35f76e3eb8"
checks["unchanged_signing_certificate"] = signer in signing.lower()
code, alignment = run([str(sdk/"zipalign.exe"),"-c","-v","4",str(apk)])
checks["zip_aligned"] = code == 0
details.update({"sha256": digest, "bytes": apk.stat().st_size,
                "signer_sha256": signer,
                "zipalign_result": alignment.splitlines()[-1:]})
out = {"timestamp_utc": datetime.now(timezone.utc).isoformat(),
       "version": "0.4.7-beta.9", "checks": checks,
       "passed": sum(checks.values()), "total": len(checks), **details}
target = root/"docs/evidence/v31/signed_verification.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
print(json.dumps(out, indent=2), flush=True)
sys.exit(0 if all(checks.values()) else 1)
