#!/usr/bin/env python3
"""«أذان من ملفي», the plain part (OwnAdhanText.java): what a file may be, and how its size is said, on tests/own_adhan_cases.tsv.
Also the manifest: the way in for the recording is shell-only and write-only. Needs javac; without it, skipped with a note.
Run: python3 tests/own_adhan_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

A = "{http://schemas.android.com/apk/res/android}"
app = ET.parse(ROOT / "android/launcher/src/main/AndroidManifest.xml").getroot().find("application")
prov = next((p for p in app.findall("provider") if p.get(A + "name") == ".tilawah.OwnAdhanProvider"), None)
check("manifest: the own-adhan door exists, is only for the shell (DUMP) and has its own authority",
      prov is not None and prov.get(A + "permission") == "android.permission.DUMP" and prov.get(A + "authorities") == "sa.urtv.home.adhan",
      None if prov is None else prov.attrib)
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/OwnAdhanProvider.java").read_text(encoding="utf-8")
check("provider: write-only, one path (own-in), nothing can be read back",
      "MODE_WRITE_ONLY" in src and '"own-in"' in src and "MODE_READ" not in src and "return null; }" in src, "")
receiver = next(r for r in app.findall("receiver") if r.get(A + "name") == ".tilawah.MusicReceiver")
check("manifest: ADHAN_OWN is on the shell-protected receiver",
      receiver.get(A + "permission") == "android.permission.DUMP"
      and "sa.urtv.home.ADHAN_OWN" in {a.get(A + "name") for a in receiver.find("intent-filter").findall("action")})
own = (ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah/OwnAdhan.java").read_text(encoding="utf-8")
check("the recording is in the app's private folder and nothing in the app sends it anywhere",
      "getFilesDir()" in own and not re.search(r"HttpURLConnection|openConnection|sendBroadcast|ACTION_SEND", own), "")
check("a recording of the person's is never uploaded by share.py", "adhan-own" not in (ROOT / "optimize/share.py").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP own adhan text: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/own_adhan_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
tile = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(tile / "OwnAdhanText.java"), str(ROOT / "tests/OwnAdhanRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.OwnAdhanRun", str(ROOT / "tests/own_adhan_cases.tsv")],
                         check=True, capture_output=True).stdout
got = out.decode("utf-8").split("\n")
for c, g in zip(cases, got):
    check(f"own adhan {c[0]}: {c[4]}", g == c[3], f"{g!r}, wanted {c[3]!r}")
check("every case answered", len(got) > len(cases), (len(got), len(cases)))
sys.exit(1 if failed else 0)
