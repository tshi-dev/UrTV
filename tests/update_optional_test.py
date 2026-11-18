#!/usr/bin/env python3
"""R-288: an optional package (the add-on) whose file is not in the release does not count in «is there a newer build», in the home's offer, or in the
download (beta 1: apk-beta held no urtv-extras.apk, the add-on at 516 kept «تحديث جديد» on and the download died with `cannot update: download`).
The rules run on a plain JVM (tests/UpdateOptionalRun.java, tests/UpdateNoticeRun.java); the wiring is read from the source.

    python3 tests/update_optional_test.py
"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
PHONE = HOME / "phone"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


if shutil.which("javac") and shutil.which("java"):
    pure = ["UpdateFlow", "UpdateOrder", "UpdateText", "UpdateRules"]
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "Tr.java")] + [str(PHONE / f"{n}.java") for n in pure] + [str(ROOT / "tests/UpdateOptionalRun.java")], capture_output=True, text=True)
        check("the rules compile", r.returncode == 0, r.stderr[-1500:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.phone.UpdateOptionalRun"], capture_output=True).stdout.decode("utf-8")
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every rule ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP rules: no javac here")

upd = text(PHONE / "PhoneUpdater.java")
prepare = upd.split("private static void prepare")[1].split("private static boolean downloadAndVerify")[0]
download = upd.split("private static boolean downloadAndVerify")[1].split("interface Job")[0]
check("the check compares the lowest build of the packages that count (not the add-on whose file is absent)",
      "UpdateRules.lowestInstalled(" in upd and "lowestInstalled(c, missingOptional(c, base))" in prepare and prepare.index("UpdateFlow.versionRead(") < prepare.index("missingOptional("))
check("the download list drops an optional package whose file is missing, with a log note", "UpdateRules.wanted(" in download and 'Log.i(TAG, "skipped' in upd)
check("missing is decided by a 404 of the probe (sizeOf), a network error stays a failure", "UpdateRules.MISSING" in upd and "UpdateRules.assetMissing(" in upd)
check("the probe runs on the update thread (never the UI thread)", 'guarded("tv-update-check"' in upd)

nrule = text(HOME / "UpdateNoticeRule.java")
notice = text(HOME / "UpdateNotice.java")
check("the home's offer: an add-on whose file is absent at the release is never behind", "boolean extrasAbsent" in nrule and "extras > 0 && !extrasAbsent && extras < t" in nrule)
check("the home's offer asks the release for the add-on's file off the main thread and keeps the answer per channel",
      "extras_absent_" in notice and "new Thread(" in notice and "UpdateRules.assetMissing(" in notice)
sys.exit(1 if failed else 0)
