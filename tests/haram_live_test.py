#!/usr/bin/env python3
"""«البث المباشر من الحرم» in «تلاوة وأذان» ← الإذاعات: two LINKS to the Saudi Broadcasting Authority's own YouTube channels, live (Makkah:
@SaudiQuranTv, Madinah: @SaudiSunnahTv, linked from sba.sa's footer), opened in the YouTube app, else SmartTube, else the browser; nothing is
streamed in our app. Read from the source, then HaramLiveRun on a JVM (needs javac; without it, skipped with a note).
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
check("the rows are in the stations tab after the stations, one per live channel, each only while its link may be shown",
      "if (HaramLive.shown(HaramLive.LIVE[h][1])) rows.add(new Row(false, KIND_LINK, sa.urtv.home.S.tv(HaramLive.LIVE[h][0]), getString(R.string.haram_live_detail), h));" in main
      and main.index("rows.add(new Row(false, KIND_STATION") < main.index("KIND_LINK, sa.urtv.home.S.tv(HaramLive.LIVE[h][0])"))
opener = main[main.index("private void openHaramLive(int which) {"):].split("\n    }\n")[0]
check("OK opens it with ACTION_VIEW, in the order HaramLive.openOrder gives (YouTube, SmartTube, the browser); nothing to open it: a line, not a crash",
      "new Intent(Intent.ACTION_VIEW, link)" in opener and "HaramLive.openOrder(" in opener and "if (pkg != null) i.setPackage(pkg);" in opener
      and "ActivityNotFoundException" in opener and "R.string.haram_live_none" in opener and "openHaramLive(row.index);" in main)
check("nothing plays in our app: the opener never touches the player, and the links are no station of the catalogue",
      "MusicService" not in opener and "openPlayer" not in opener and "HaramLive" not in text(TIL / "MusicService.java")
      and "HaramLive" not in text(TIL / "Catalog.java"))
check("the words: where it opens, and the line when it cannot; the titles marked for the table",
      "في تطبيقها أو المتصفح" in strings and '<string name="haram_live_none">' in strings
      and 'S.n("مكة المكرمة · قناة القرآن الكريم")' in main and 'S.n("المدينة المنورة · قناة السنة النبوية")' in main)
check("the links are SBA's two official /live channel pages (no aggregator, no stream address, no video id)",
      '"https://www.youtube.com/@SaudiQuranTv/live"' in live and '"https://www.youtube.com/@SaudiSunnahTv/live"' in live
      and "m3u8" not in live.lower() and "makkahlive" not in live.lower() and "watch?v=" not in live)
if not shutil.which("javac"):
    print("SKIP haram live: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", t, str(TIL / "HaramLive.java"), str(ROOT / "tests/HaramLiveRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", t, "sa.urtv.home.tilawah.HaramLiveRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
