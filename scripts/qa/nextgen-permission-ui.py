"""Permission dialog verification on the disposable hosted API35 emulator."""
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = pathlib.Path("qa-runtime-evidence")
PKG = "digital.alghad.protechmusic"
PERM = "android.permission.READ_MEDIA_AUDIO"

def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, timeout=25)

def package_state(name):
    state = adb("shell", "dumpsys", "package", PKG)
    (OUT / (name + ".txt")).write_text(state, encoding="utf-8")
    assert PERM in state, "permission not declared"
    return bool(re.search(r"android\.permission\.READ_MEDIA_AUDIO:\s*granted=true", state))

def locate(name, predicate, timeout=40):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/nextgen-permissions.xml")
            xml = adb("shell", "cat", "/sdcard/nextgen-permissions.xml")
            (OUT / (name + ".xml")).write_text(xml, encoding="utf-8")
            root = ET.fromstring(xml)
            parents = {child: parent for parent in root.iter() for child in parent}
            for node in root.iter("node"):
                attrs = node.attrib
                if predicate(attrs):
                    # Compose exposes the label as a non-clickable child of the actual button.
                    target = node
                    while target.attrib.get("clickable") != "true" and target in parents:
                        target = parents[target]
                    if target.attrib.get("clickable") != "true":
                        continue
                    x1, y1, x2, y2 = map(int, re.findall(r"\d+", target.attrib["bounds"]))
                    assert x2 > x1 and y2 > y1
                    return (str((x1+x2)//2), str((y1+y2)//2))
        except (subprocess.SubprocessError, ET.ParseError):
            pass
        time.sleep(1)
    (OUT / (name + ".png")).write_bytes(subprocess.check_output(["adb", "exec-out", "screencap", "-p"], timeout=25))
    raise RuntimeError("Missing visible UI: " + name)

def tap(point):
    adb("shell", "input", "tap", *point)

def grant_button(a):
    return a.get("text") in ("Allow audio", "\u0645\u0646\u062d \u0635\u0644\u0627\u062d\u064a\u0629 \u0627\u0644\u0635\u0648\u062a\u064a\u0627\u062a") and a.get("package") == PKG

def dialog_button(suffix):
    return lambda a: a.get("package", "").endswith("permissioncontroller") and a.get("resource-id", "").endswith(":id/" + suffix)

assert not package_state("permissions-before")
adb("shell", "am", "start", "-W", "-n", PKG + "/.MainActivity")
def open_dialog(label, suffix):
    # Wait until the actual button is visible; retry only while the app button exists.
    for attempt in range(3):
        tap(locate(label + f"-{attempt}", grant_button))
        try:
            return locate("permission-" + suffix + f"-{attempt}", dialog_button(suffix), timeout=10)
        except RuntimeError:
            if attempt == 2:
                raise
    raise AssertionError("unreachable")

time.sleep(2)
tap(open_dialog("app-grant-button", "permission_deny_button"))
assert not package_state("permissions-denied")
tap(open_dialog("app-grant-again", "permission_allow_button"))
for _ in range(10):
    if package_state("permissions-after"):
        break
    time.sleep(1)
else:
    raise RuntimeError("Permission not granted after dialog interaction")
print("PERMISSION_UI_PASS deny, request again, allow")
