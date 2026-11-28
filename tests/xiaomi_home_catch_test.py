#!/usr/bin/env python3
"""R-405: on Xiaomi's Google TVs the Home key went on opening Google TV's home, because the add-on's Home catch is off on every Xiaomi (R-310).
A per-model allow-list (RemoteButtons.XIAOMI_GTV_HOME_CATCH: the Xiaomi Google TVs of docs/TVS.md) turns it back on there; every other Xiaomi
keeps R-310's rule, and the app buttons' filter stays off on all of them. tests/XiaomiHomeCatchRun.java runs it. Run: python3 tests/xiaomi_home_catch_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

rb = (HOME / "RemoteButtons.java").read_text(encoding="utf-8")
kb = (HOME / "KeyButton.java").read_text(encoding="utf-8")
tvs = (ROOT / "docs/TVS.md").read_text(encoding="utf-8")
m = re.search(r"XIAOMI_GTV_HOME_CATCH = \{([^}]*)\}", rb)
models = re.findall(r'"([^"]+)"', m.group(1)) if m else []
check("the allow-list is there", len(models) >= 1, models)
for x in models:
    check(x + " is a Xiaomi of docs/TVS.md", x in tvs or x.replace("AFMU1", "AFMU0/1").split("-")[1] in tvs)
check("the add-on asks with the TV's model", "RemoteButtons.homeCatchSafe(TV, Build.MODEL)" in kb)
if not shutil.which("javac"):
    print("SKIP XiaomiHomeCatchRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "RemoteButtons.java"), str(ROOT / "tests/XiaomiHomeCatchRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.XiaomiHomeCatchRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of XiaomiHomeCatchRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
