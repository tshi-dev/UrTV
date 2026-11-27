#!/usr/bin/env python3
"""Review F95: a paired-route connection nobody closed (the home was killed mid-setup) must not make every open answer «busy» for good."""
import shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PAIR = ROOT / "android/extras/src/main/java/sa/urtv/home/pair"
if not shutil.which("javac"):
    print("SKIP pair pool: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(PAIR / "ConnPool.java"), str(ROOT / "tests/ConnPoolRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.pair.ConnPoolRun"], check=True, capture_output=True).stdout.decode().splitlines())
want = {"two": "2 []", "full evicts the oldest": "2 [c1] null c3", "never over max": "2", "remove closes": "1 true", "unknown ignored": "1", "clear": "0 true"}
failed = 0
for k, v in want.items():
    ok = got.get(k) == v
    print(("PASS" if ok else "FAIL") + f" pair pool: {k}" + ("" if ok else f"  -> {got.get(k)!r}, wanted {v!r}"))
    failed += not ok
src = (PAIR / "PairHost.java").read_text(encoding="utf-8")
ok = "ConnPool" in src and 'throw new IOException("busy")' not in src
print(("PASS" if ok else "FAIL") + " pair pool: PairHost uses it and no longer answers busy")
failed += not ok
sys.exit(1 if failed else 0)
