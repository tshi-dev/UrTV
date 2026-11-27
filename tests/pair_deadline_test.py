#!/usr/bin/env python3
"""Review F96: a stalled Wi-Fi link must not hang the setup thread and the add-on's binder thread for good: a call on the paired route ends with «timeout» and closes the connection."""
import shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PAIR = ROOT / "android/extras/src/main/java/sa/urtv/home/pair"
if not shutil.which("javac"):
    print("SKIP pair deadline: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(PAIR / "Deadline.java"), str(ROOT / "tests/DeadlineRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.pair.DeadlineRun"], check=True, capture_output=True, timeout=60).stdout.decode().splitlines())
want = {"fast": "ok", "stalled": "timeout true true", "io error passes": "closed"}
failed = 0
for k, v in want.items():
    ok = got.get(k) == v
    print(("PASS" if ok else "FAIL") + f" pair deadline: {k}" + ("" if ok else f"  -> {got.get(k)!r}, wanted {v!r}"))
    failed += not ok
src = (PAIR / "PairHost.java").read_text(encoding="utf-8")
ok = "Deadline.call(() -> conn.run(command)" in src and "Deadline.call(() -> conn.exec(command, in, size)" in src
print(("PASS" if ok else "FAIL") + " pair deadline: PairHost.run and exec go through it")
failed += not ok
sys.exit(1 if failed else 0)
