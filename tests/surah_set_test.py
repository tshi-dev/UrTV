#!/usr/bin/env python3
"""4.3.3: a reciter who did not record all 114 surahs. reciters.json may give «missing» (the surah numbers with no recording); «surahs» is then
114 minus them. SurahSet (run for real, tests/SurahSetRun.java) steps over a missing surah in next, previous, the end of a surah and a pick;
Catalog reads the list, MusicService plays through SurahSet, the surah grid keeps all 114 in their places with a missing one dimmed
«غير متوفرة» and not played, and the phone page gets the list. Old reciters (no «missing») play exactly as before. Needs javac for the run.
    python3 tests/surah_set_test.py"""
import json, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

reciters = json.loads((ROOT / "android/launcher/src/main/assets/quran/reciters.json").read_text(encoding="utf-8"))["reciters"]
bad = [r["id"] for r in reciters if "missing" in r and not (isinstance(r["missing"], list) and r["missing"] and all(isinstance(n, int) and 1 <= n <= 114 for n in r["missing"])
                                                         and r["missing"] == sorted(set(r["missing"])) and len(r["missing"]) < 114)]
check("«missing», where given, is a sorted list of surah numbers 1 to 114 without repeats, never all of them", not bad, bad)
bad = [r["id"] for r in reciters if r["surahs"] != 114 - len(r.get("missing", []))]
check("«surahs» is 114 minus the missing ones", not bad, bad)

hum = [r for r in reciters if r["id"] == "humaid"]
check("Ahmed Talib bin Humaid: 107 surahs, the seven he has not recorded marked missing (mp3quran id 137)",
      len(hum) == 1 and hum[0]["surahs"] == 107 and hum[0]["missing"] == [9, 14, 16, 17, 23, 24, 33]
      and hum[0]["server"] == "https://cdn.mp3quran.net/audio/ahmad-humaid/r1/" and hum[0]["name"] == "أحمد طالب بن حميد", hum)
cat = (T / "Catalog.java").read_text(encoding="utf-8")
svc = (T / "MusicService.java").read_text(encoding="utf-8")
grid = (T / "SurahActivity.java").read_text(encoding="utf-8")
phone = (T / "PhoneBridge.java").read_text(encoding="utf-8")
sset = (T / "SurahSet.java").read_text(encoding="utf-8")
check("SurahSet is plain Java (no Android import)", "import android" not in sset)
check("Catalog reads «missing» (optional) and counts from it", 'r.optJSONArray("missing")' in cat and "SurahSet.count(this.missing)" in cat and "SurahSet.clean(missing)" in cat)
check("the player steps over a missing surah: a pick, next and previous",
      "SurahSet.atOrAfter(n, r.missing)" in svc and "SurahSet.next(surah, cat.reciters.get(reciter).missing)" in svc and "SurahSet.prev(surah, cat.reciters.get(reciter).missing)" in svc)
check("the old 1..surahs wrap is gone (a count is not the last number any more)", "r.surahs" not in svc and ".surahs;" not in svc)
check("a remembered place is kept unless the surah changed", "if (n != asked) startAt = 0;" in svc)
check("the grid keeps 114 places, a missing one dimmed «غير متوفرة» and not played",
      "return SurahSet.LAST;" in grid and 'S.t("غير متوفرة")' in grid and "setAlpha(has ? 1f : 0.4f)" in grid and "if (!reciter.has(position + 1))" in grid)
check("the phone page gets the list (only for a reciter that has one)", 'm.put("missing", gone)' in phone and "if (r.missing.length > 0)" in phone)
tsv = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
check("the new words have English", "غير متوفرة\tNot available" in tsv and "هذه السورة غير متوفرة بصوت هذا القارئ\t" in tsv)

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "SurahSet.java"), str(ROOT / "tests/SurahSetRun.java")], capture_output=True, text=True)
        check("SurahSet compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.SurahSetRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 15, len(out.splitlines()))
sys.exit(1 if failed else 0)
