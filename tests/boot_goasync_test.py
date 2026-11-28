#!/usr/bin/env python3
"""F133 (the 4.4 review, batch 1): the boot receiver calls goAsync() ONCE per broadcast (a second call returns null; finishing it threw a NullPointerException in a
thread: a crash at boot on a TV with a pending safety switch or a factory restore). One goAsync, shared by every task through AsyncCount (tests/AsyncCountRun.java,
plain Java).   python3 tests/boot_goasync_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SETUP / "AsyncCount.java"), str(ROOT / "tests/AsyncCountRun.java")], capture_output=True, text=True)
        check("AsyncCount compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.AsyncCountRun"], capture_output=True, text=True, encoding="utf-8").stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
else:
    print("FAIL no javac or java")
    failed += 1

src = (SETUP / "SetupBootReceiver.java").read_text(encoding="utf-8")
code = "\n".join(l for l in src.splitlines() if not l.lstrip().startswith(("*", "//", "/*")))
check("the boot receiver calls goAsync() exactly once (in onReceive)", len(re.findall(r"\bgoAsync\(\)", code)) == 1 and code.index("goAsync()") < code.index("factoryGuard(app, count)"))
check("every task takes a hold before its thread and lets go in finally; onReceive lets go once at its end",
      code.count("count.hold();") >= 2 and code.count("count.release();") >= 3 and "pending.finish()" in code and code.count("pending.finish()") == 1)
sys.exit(1 if failed else 0)
