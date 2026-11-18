#!/usr/bin/env python3
"""The games' bundle (GameBundle.java, games/tools/pack_bundle.py): made in Python, opened on a plain JVM; and what docs/LICENSE-CONTENT.md promises."""
import json, os, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
sys.path.insert(0, str(ROOT / "games/tools"))
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
if not shutil.which("javac"): print("SKIP: no javac"); sys.exit(1)
import pack_bundle as pb
with tempfile.TemporaryDirectory() as t:
    t = Path(t)
    text = json.dumps({"v": 1, "items": [{"q": "ما عاصمة مصر؟", "a": "القاهرة", "n": i} for i in range(300)]}, ensure_ascii=False).encode("utf-8")
    (t / "plain.json").write_bytes(text)
    blob = pb.pack("demo", text); (t / "demo.gb").write_bytes(blob)
    check("Python: pack then unpack gives the text back; two packs of the same text differ (a fresh IV each time)", pb.unpack("demo", blob) == text and pb.pack("demo", text) != blob)
    check("the file is smaller than the text and shows no readable Arabic or JSON", len(blob) < len(text) and "القاهرة".encode() not in blob and b"items" not in blob)
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", str(t), str(APP / "GameBundle.java"), str(ROOT / "tests/GameBundleTest.java")], capture_output=True, text=True)
    check("GameBundle compiles on a plain JVM", c.returncode == 0, c.stderr[-400:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", str(t), "sa.urtv.screensaver.GameBundleTest", "demo", str(t / "demo.gb"), str(t / "plain.json")], capture_output=True, text=True, timeout=60)
        for l in r.stdout.splitlines():
            if l.startswith(("PASS", "FAIL")):
                print(l); failed += l.startswith("FAIL")
sys.exit(1 if failed else 0)
