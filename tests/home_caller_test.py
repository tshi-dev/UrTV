#!/usr/bin/env python3
"""R-412 (a TCL on Android 11): inside the TV's settings one option sent the person back to our home, and nothing said what fired HOME. Each HOME start
of our home now logs the kind of app that asked for it (MainActivity.logHomeCaller, Activity.getReferrer, HomeCaller.word: fixed words, never an app
the person installed by name). Diagnosis only: nothing is changed. Run: python3 tests/home_caller_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
ni = main.split("protected void onNewIntent(Intent intent) {")[1].split("\n    }\n")[0]
check("onNewIntent logs the caller first", "logHomeCaller(intent);" in ni and ni.index("logHomeCaller(intent);") < ni.index("Parental.SESSION.clear();"))
lh = main.split("private void logHomeCaller(Intent intent) {")[1].split("\n    }\n")[0] if "logHomeCaller(Intent intent) {" in main else ""
check("only for a HOME start, the referrer's word only, never throws", "Intent.CATEGORY_HOME" in lh and '"home started by: " + HomeCaller.word(pkg)' in lh
      and "catch (RuntimeException ignored)" in lh and "SDK_INT >= 22" in lh)
check("it changes nothing (no finish, no start, no move)", not any(w in lh for w in ("finish(", "startActivity(", "moveTaskToBack(")))
if not shutil.which("javac"):
    print("SKIP HomeCallerRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(HOME / "HomeCaller.java"), str(ROOT / "tests/HomeCallerRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.HomeCallerRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of HomeCallerRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
