#!/usr/bin/env python3
"""R-406: Netflix, Prime Video and the maker's app buttons did nothing over other apps once UrTV was the home; only a TCL's YouTube button was
answered by the add-on. Now every TCL button with an app of its own (YouTube, Netflix 4062, Prime Video 4099) opens it from the add-on, and on a
Xiaomi Google TV of RemoteButtons.XIAOMI_GTV_HOME_CATCH (where the Home catch keeps a key filter on, R-405) its YouTube button does too; each
only when the app is installed (else the key goes on, logged), behind the family lock (openKeyApp). Owner choices stay off on every Xiaomi
(R-310). tests/AppKeysDefaultRun.java runs the rules. Run: python3 tests/app_keys_default_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

svc = (ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java").read_text(encoding="utf-8")
ak = svc.split("private boolean appKey(KeyEvent e) {")[1].split("\n    }\n")[0]
xk = svc.split("private boolean xiaomiGtvKey(KeyEvent e) {")[1].split("\n    }\n")[0] if "xiaomiGtvKey(KeyEvent e) {" in svc else ""
fk = svc.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("a Xiaomi Google TV's keys go to xiaomiGtvKey first; R-310's gate stays for every other TV", ak.strip().startswith("if (KeyButton.xiaomiGtv()) return xiaomiGtvKey(e);")
      and "if (!KeyButton.filterSafe()) return false;" in ak)
check("xiaomiGtvKey: only a button with an app of its own; no owner choice; installed or the key goes on; the family lock (openKeyApp)",
      "KeyApps.xiaomiGtvOwn(KeyButton.TV, true, id," in xk and "keyChoices" not in xk and "launchIntent(this, own) == null" in xk and "openKeyApp(pkg)" in xk
      and "KeyButton.burstMs()" in xk)
check("the add-on asks for keys there while our home is the person's", "KeyApps.xiaomiGtvWanted(KeyButton.xiaomiGtv(), wanted(this) || oursChosen())" in fk)
check("both copies of RemoteButtons have xiaomiGtv", all("static boolean xiaomiGtv(int tv, String model)" in p.read_text(encoding="utf-8")
      for p in (HOME / "RemoteButtons.java", ROOT / "android/app/src/main/java/sa/urtv/screensaver/RemoteButtons.java")))

if not shutil.which("javac"):
    print("SKIP AppKeysDefaultRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "RemoteButtons.java"), str(HOME / "KeyApps.java"), str(ROOT / "tests/AppKeysDefaultRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.AppKeysDefaultRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of AppKeysDefaultRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
