#!/usr/bin/env python3
"""R-504 (docs/research/LAUNCHERS-GITHUB.md idea 7): the Home button of some Xiaomi remotes sends KEYCODE_ALL_APPS (284), not KEYCODE_HOME, so the
add-on's Home catch never saw it. On a Xiaomi TV it is the Home key, only where the catch is on (homeKeyActive: the person's home ours, and the TVs
where the catch may filter keys); elsewhere nothing changes. RemoteButtons.homeKey on a plain JVM, then the wiring.
Needs javac for the Java cases. Run: python3 tests/all_apps_home_test.py"""
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


rb = (HOME / "RemoteButtons.java").read_text(encoding="utf-8")
check("RemoteButtons.homeKey exists", "static boolean homeKey(int keyCode, String manufacturer, String brand)" in rb)
if "static boolean homeKey(" in rb and shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "RemoteButtons.java"), str(ROOT / "tests/HomeKeyCodesRun.java")], capture_output=True, text=True)
    check("RemoteButtons compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.HomeKeyCodesRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every homeKey case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")
check("KEYCODE_ALL_APPS is 284 and KEYCODE_HOME 3 (android.view.KeyEvent)", "KEY_HOME = 3, KEY_ALL_APPS = 284;" in rb)
svc = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
check("the add-on catches it as Home only while the catch is on, after the voice key and before the app buttons",
      "RemoteButtons.homeKey(e.getKeyCode(), Build.MANUFACTURER, Build.BRAND) && homeKeyActive()) return homeKeyCaught(e);" in svc
      and svc.index("VoiceKeys.is(e.getKeyCode())") < svc.index("RemoteButtons.homeKey(") < svc.index("if (appKey(e)) return true;"))
print("ALL PASS" if not failed else f"{failed} FAILED")
raise SystemExit(1 if failed else 0)
