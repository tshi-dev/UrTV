#!/usr/bin/env python3
"""«البث المباشر من الحرم» in «تلاوة وأذان» ← الإذاعات: a LINK to the broadcaster's own live page or channel, opened in its own app or the
browser; nothing is streamed in our app. The official link could not be confirmed from here, so HaramLive.URL is empty and the row stays
hidden until it is filled in. Read from the source, then HaramLiveRun on a JVM (needs javac; without it, skipped with a note).
Run: python3 tests/haram_live_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TIL = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
text = lambda p: p.read_text(encoding="utf-8")
main, live = text(TIL / "MainActivity.java"), text(TIL / "HaramLive.java")
strings = text(ROOT / "android/launcher/src/main/res/values/strings.xml")
check("the row is in the stations tab after the stations, only while the link may be shown",
      "if (HaramLive.shown(HaramLive.URL)) rows.add(new Row(false, KIND_LINK, getString(R.string.haram_live), getString(R.string.haram_live_detail), -1));" in main
      and main.index("rows.add(new Row(false, KIND_STATION") < main.index("KIND_LINK, getString(R.string.haram_live)"))
opener = main[main.index("private void openHaramLive() {"):].split("\n    }\n")[0]
check("OK opens it with ACTION_VIEW in its own app or the browser; a TV with nothing to open it gets a line, not a crash",
      "startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HaramLive.URL))" in opener and "ActivityNotFoundException" in opener
      and "R.string.haram_live_none" in opener and "else if (row.kind == KIND_LINK) {\n                openHaramLive();" in main)
check("nothing plays in our app: the opener never touches the player, and the link is no station of the catalogue",
      "MusicService" not in opener and "openPlayer" not in opener and "HaramLive" not in text(TIL / "MusicService.java")
      and "HaramLive" not in text(TIL / "Catalog.java"))
check("the words: «البث المباشر من الحرم», where it opens, and the line when it cannot",
      '<string name="haram_live">البث المباشر من الحرم</string>' in strings and "في تطبيقها أو المتصفح" in strings
      and '<string name="haram_live_none">' in strings)
check("the link is a constant, empty until the official one is confirmed (no aggregator, no stream address)",
      'static final String URL = "";' in live and "m3u8" not in live.lower() and "makkahlive" not in live.lower())
if not shutil.which("javac"):
    print("SKIP haram live: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(TIL / "HaramLive.java"), str(ROOT / "tests/HaramLiveRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, "sa.urtv.home.tilawah.HaramLiveRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
