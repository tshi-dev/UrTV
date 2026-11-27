#!/usr/bin/env python3
"""4.4 beta 4, review F87 (R-262 back): «إلغاء» on «Allow USB debugging?» ends as the Cancel's own message with its retry, also when the paired route exists. SetupSession.openAny
rethrew the 5555 failure with «; paired route: <why>» added, and SetupRunner / friendly() compared it with equals, so a Cancel became «انقطع الاتصال» with no retry.
Run: python3 tests/setup_cancel_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)[:500]))
    failed += not ok
rd = lambda p: Path(p).read_text(encoding="utf-8")

ss, runner, adb = rd(SETUP / "SetupSession.java"), rd(SETUP / "SetupRunner.java"), rd(SETUP / "AdbClient.java")
check("openAny: a Cancel (or the run's end) is rethrown as it was before the paired route is asked, and every other word added goes through PairFallback.message",
      "if (PairFallback.answered(first.getMessage())) throw first;" in ss and "PairFallback.message(first.getMessage(), why)" in ss and 'first.getMessage() + "; paired route: "' not in ss)
check("the runner and friendly() read a Cancel by its start, never by equals", "AdbClient.cancelled(m)" in runner and "AdbClient.cancelled(ex.getMessage())" in runner and "CANCELLED.equals(" not in runner and "m.equals(AdbClient.CANCELLED)" not in runner
      and "static boolean cancelled(String message)" in adb)
if not shutil.which("javac"):
    print("SKIP the fallback on a JVM: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java", SETUP.parent / "SuggestedApps.java", SETUP.parent / "ExtrasWords.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/PairFallbackRun.java")], capture_output=True, text=True)
    check("PairFallback and the runner compile on a JVM (no Android in them)", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.PairFallbackRun"], capture_output=True, text=True, encoding="utf-8").stdout.splitlines()
        for line in out:
            n, res = line.rsplit("\t", 1)
            check(n, res == "ok")
        check("every check ran", len(out) == 8, len(out))
sys.exit(1 if failed else 0)
