#!/usr/bin/env python3
"""4.3: the home offers «تحديث تلفزيوني» on the TV when the screensaver app is older than the home, or a newer build is out: the rules (UpdateNoticeRule) run in
Java, and the wiring (a pill in the top row, the release read off the main thread at most every 12 hours, nothing installed from the home).

    python3 tests/update_notice_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "UpdateNoticeRule.java"), str(ROOT / "tests/UpdateNoticeRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.UpdateNoticeRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every rule ran", "ALL PASS" in out, out[-200:])
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
un = (HOME / "UpdateNotice.java").read_text(encoding="utf-8")
check("a pill «تحديث جديد» opens the update screen (behind its lock) and is in the row only while UpdateNotice says so, never in kids mode",
      'new Intent(this, UpdateActivity.class)' in main and "UpdateNotice.wanted(this)" in main and "!Parental.state(this).kids && UpdateNotice.wanted(this)" in main)
check("the release is read off the main thread, at most every 12 hours, only its first 64 bytes; nothing is installed from the notice",
      "new Thread(" in un and "byte[] buf = new byte[64];" in un and "UpdateNoticeRule.due(" in un and "PackageInstaller" not in un and "ACTION_VIEW" not in un)
sys.exit(1 if failed else 0)
