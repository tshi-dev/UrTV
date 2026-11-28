#!/usr/bin/env python3
"""4.4 beta 4: a photo sent from the phone arrived twice in «صوري». The same bytes as a kept photo is that photo: Own.keep finds it (PhotoTwin, SHA-256
after a length check) and keeps nothing new, and the add is done (the phone's retry is not an error); a phone retry while «صوري» is full is not
refused as «full» either. PhotoTwin (no Android) on files made here, then the wiring in Own and ConfigProvider.
Run: python3 tests/photo_twin_test.py"""
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

own = (APP / "Own.java").read_text(encoding="utf-8")
prov = (APP / "ConfigProvider.java").read_text(encoding="utf-8")
keep = own[own.index("private static synchronized String keep("):]
keep = keep[:keep.index("\n    }\n")]
check("Own.keep looks for the twin first (before «full», before the copy), and drops the incoming file", "twin(c, src) > 0" in keep
      and keep.index("twin(c, src)") < keep.index("count(c) >= max(c)") and "src.delete()" in keep[:keep.index("count(c) >= max(c)")])
check("a twin is a done add (null), so the phone and photos.sh report it kept", "return null;" in keep[:keep.index("count(c) >= max(c)")])
check("Own.twin compares with every kept photo, 1.jpg to the count", "PhotoTwin.find(src, kept)" in own and "kept[i] = file(c, i + 1)" in own)
check("the phone's add: a twin while «صوري» is full is not «full»", "Own.count(c) >= Own.max(c) && !Own.sameBytes(c, jpeg)" in prov)
twin = (APP / "PhotoTwin.java").read_text(encoding="utf-8")
check("PhotoTwin is pure (no android import) and reads a kept photo only when its length is the same",
      "import android" not in twin and "kept[i].length() != len" in twin and 'MessageDigest.getInstance("SHA-256")' in twin)

if not shutil.which("javac"):
    print("SKIP photo twin: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)
    r = subprocess.run(["javac", "-d", str(t / "c"), str(APP / "PhotoTwin.java"), str(ROOT / "tests/PhotoTwinRun.java")], capture_output=True, text=True)
    check("PhotoTwin compiles", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        a = os.urandom(5000); b = bytearray(a); b[100] ^= 1
        files = {"a": a, "a2": a, "b": bytes(b), "short": a[:4000], "empty": b""}
        for k, v in files.items(): (t / k).write_bytes(v)
        def find(src, *kept):
            return subprocess.run(["java", "-cp", str(t / "c"), "sa.urtv.screensaver.PhotoTwinRun", str(t / src), *[str(t / k) for k in kept]],
                                  capture_output=True, text=True).stdout.strip()
        check("the same bytes as the 2nd kept photo: 2", find("a2", "b", "a", "short") == "2")
        check("one byte different, same length: no twin", find("b", "a", "short") == "0")
        check("a shorter photo with the same start: no twin", find("short", "a") == "0")
        check("nothing kept: no twin", find("a") == "0")
        check("a kept photo that is gone: skipped, the next is found", find("a", "missing", "a2") == "2")
sys.exit(1 if failed else 0)
