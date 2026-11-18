#!/usr/bin/env python3
"""4.4 «تلاوة وأذان», from an audit of the screen and the player:
  - each surah keeps its place (SurahPlaces, the last 20 heard): a pick of a surah heard half way goes on from there (before, every pick started
    at 0:00 and wiped the one «متابعة» place); a surah that ended forgets it; the next surah after an end starts at 0;
  - the place is saved every 30 s while it plays (a power cut kept only the last pause);
  - the sleep timer never cuts the adhan (it waits, then fades what the adhan brought back); OK during its fade is «I am awake»: the timer ends,
    the sound is back, it plays on (it used to stop everything);
  - a service Android ended is not «يُتلى الآن» any more; a new sleep time is said to the card at once;
  - the letters: entering the column keeps the list on the person's reciter; a letter lands among the rest before the ★ section;
  - the card's keys follow Dir (Arabic: ▶ back), as the surah grid and the tabs.
SurahPlaces runs for real.
    python3 tests/surah_places_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, prefs, act, order = read(T / "MusicService.java"), read(T / "Prefs.java"), read(T / "MainActivity.java"), read(T / "ReciterOrder.java")
home = read(ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java")

check("a pick of a surah goes on from its place", "playSurahById(rid, n, SurahPlaces.get(prefs.surahPlaces(), rid, n));" in svc)
check("the place is kept with every save, and an ended surah forgets it", "prefs.setSurahPlaces(SurahPlaces.put(prefs.surahPlaces(), id, surah, position, duration))" in svc
      and "if (source == Prefs.SOURCE_SURAH) { forgetPlace(); next(); }" in svc and '"surah_places"' in prefs)
check("saved every 30 s while it plays (never the adhan)", "SAVE_EVERY_MS = 30_000" in svc and "if (playing && source != Prefs.SOURCE_ADHAN && SystemClock.elapsedRealtime() - lastSaved >= SAVE_EVERY_MS) save();" in svc)
check("the sleep timer waits for the adhan", "if (source == Prefs.SOURCE_ADHAN) { handler.postDelayed(this, 5_000); return; }" in svc)
check("OK during the fade: awake (the timer ends, the volume back, it plays on)", "if (fadeStep > 0) { setSleep(0); return; }" in svc.split("void toggle() {")[1].split("}")[0] + "}")
destroy = svc[svc.index("@Override public void onDestroy()"):svc.index("// ---- playing ----")]
check("a service Android ended clears what the card reads, and says so", "source = Prefs.SOURCE_NONE;" in destroy and "playing = false;" in destroy and "sleepUntil = 0;" in destroy and "announce();" in destroy)
check("a new sleep time reaches the card at once", '(sleepUntil / 60_000)' in svc)
check("entering the letters keeps the row; moving to a letter jumps", "if (has && !enteringLetters) jumpToLetter(which);" in act and "try { letterViews[target].requestFocus(); } finally { enteringLetters = false; }" in act)
check("a letter lands among the rest before the ★ section", "it.fav == (pass == 1)" in order)
check("the card's keys follow Dir", "if (e.getKeyCode() == Dir.backKey()) NowPlaying.prev(this); else NowPlaying.next(this);" in home)

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class SpRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  String r = "";
  r = SurahPlaces.put(r, "sudais", 2, 4_200_000, 9_000_000);
  r = SurahPlaces.put(r, "afasy", 18, 600_000, 1_500_000);
  out("each surah its own place, any reciter", SurahPlaces.get(r, "sudais", 2) == 4_200_000 && SurahPlaces.get(r, "afasy", 18) == 600_000 && SurahPlaces.get(r, "afasy", 2) == 0);
  out("the latest first", r.startsWith("afasy:18:"));
  r = SurahPlaces.put(r, "sudais", 2, 4_300_000, 9_000_000);
  out("a new place replaces the old, moved first", r.startsWith("sudais:2:4300000") && r.split(";").length == 2);
  out("near the start or the end: no place", SurahPlaces.get(SurahPlaces.put(r, "sudais", 2, 5_000, 9_000_000), "sudais", 2) == 0
      && SurahPlaces.get(SurahPlaces.put(r, "sudais", 2, 8_990_000, 9_000_000), "sudais", 2) == 0);
  out("an ended surah forgets it", SurahPlaces.get(SurahPlaces.forget(r, "afasy", 18), "afasy", 18) == 0 && SurahPlaces.get(SurahPlaces.forget(r, "afasy", 18), "sudais", 2) == 4_300_000);
  String many = "";
  for (int i = 1; i <= 25; i++) many = SurahPlaces.put(many, "sudais", i, 60_000, 0);
  out("only the last 20 kept", many.split(";").length == SurahPlaces.KEEP && SurahPlaces.get(many, "sudais", 25) == 60_000 && SurahPlaces.get(many, "sudais", 1) == 0);
  out("a broken record or a bad key is nothing, never thrown", SurahPlaces.get("x;;sudais:2:abc;:1:2", "sudais", 2) == 0 && SurahPlaces.get(null, "sudais", 2) == 0
      && SurahPlaces.get(r, "", 2) == 0 && SurahPlaces.get(r, "sudais", 115) == 0 && SurahPlaces.put(r, "a;b", 2, 60_000, 0).equals(r));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "SpRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "SurahPlaces.java"), str(T / "SurahSet.java"), str(Path(tmp, "SpRun.java"))], capture_output=True, text=True)
        check("SurahPlaces compiles on its own (no Android)", r.returncode == 0 and "import android" not in read(T / "SurahPlaces.java"), r.stderr[-500:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.SpRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 7, len(out.splitlines()))
sys.exit(1 if failed else 0)
