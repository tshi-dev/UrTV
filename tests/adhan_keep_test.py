#!/usr/bin/env python3
"""4.4 beta 3 «الأذان محفوظ مسبقاً» (R-302): the usual and the Fajr adhan recordings are kept in the app's own files (not the cache Android may empty), checked for
from the home's minute tick on their own thread (an hour after a failed try, ten minutes in the last 45 before a prayer); at the prayer a voice that is not on
the TV while offline, or whose stream fails, gives way to another saved voice; an adhan that could not play at all is said in «الأذان» for a day.
AdhanKeep runs on a plain JVM; the wiring is read from the sources.
    python3 tests/adhan_keep_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


ad = (T / "Adhan.java").read_text(encoding="utf-8")
ms = (T / "MusicService.java").read_text(encoding="utf-8")
sync = (T / "AdhanSync.java").read_text(encoding="utf-8")
sched = (T / "AdhanScheduler.java").read_text(encoding="utf-8")
main = (T / "MainActivity.java").read_text(encoding="utf-8")
en = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")

check("the kept copies live in the app's files (getFilesDir), the others stay in the cache", 'new File(new File(c.getFilesDir(), "adhan"), "adhan-"' in ad and 'new File(new File(c.getCacheDir(), "adhan"), "adhan-"' in ad)
check("the player plays the kept copy first (cached() prefers it) and a copy anywhere counts (isCached)",
      "return kept.length() > MIN_BYTES ? kept : inCache(c, voice);" in ad and "kept(c, voice).length() > MIN_BYTES || inCache(c, voice).length() > MIN_BYTES" in ad)
tick = ad.split("static void keepCheck(Context c) {")[1].split("\n    }\n")[0]
check("the minute tick only hands off to the adhan-keep thread (nothing on the UI thread)", "keeper.execute(() -> keepNow(app, TICK));" in tick and "File" not in tick and '"adhan-keep"' in ad)
pc = ad.split("static void prefetchChosen(final Context c) {")[1].split("\n    }\n")[0]
check("a voice change or a new alarm hands off too (no file looked at on the caller's thread)", "keeper.execute(() -> keepNow(app, ASKED));" in pc and "isCached" not in pc)
kn = ad.split("private static void keepNow(Context c, int who) {")[1].split("\n    }\n")[0]
check("the check: cache copies moved in, unchosen ones moved out, the saved list written, a fetch only when due and the screen is on",
      "moveIn(c, w)" in kn and "tidy(c, wanted);" in kn and "prefs.setAdhanSaved(AdhanKeep.savedText(saved));" in kn
      and "AdhanKeep.due(true, now, prefs.prefetchTried(), prefs.nextAdhanAt())" in kn and "NetGate.backgroundOk" in kn
      and kn.index("NetGate.backgroundOk") < kn.index("keepOne(c, w)") and "prefs.setPrefetchTried(now);" in kn)
check("a fetch of a chosen voice writes straight into the kept folder (through .part, whole files only)", "return fetch(c, voice, kept(c, voice));" in ad and 'file.getName() + ".part"' in ad)
check("the home's minute tick asks (AdhanSync, adhan on); the alarm writes the next adhan's time", "if (new Prefs(c).adhanOn()) Adhan.keepCheck(c);" in sync and "new Prefs(c).setNextAdhanAt(next.at);" in sched)
check("«تلاوة» opened: the chosen ones are kept now, on the keep thread, before the daily round", "keeper.submit(() -> keepNow(c, NOW)).get();" in ad)
start = ms.split("prefs.setLastAdhan(System.currentTimeMillis());")[1].split("if (previewSeconds > 0)")[0]
check("at the prayer: not saved and offline: a saved voice plays at once (not a stream that cannot come)", "} else if (!online() && (stand = standIn(voice)) > 0) {" in start and "playStandIn(stand);" in start)
fail = ms.split("if (source == Prefs.SOURCE_ADHAN && retries >= 2) {")[1].split("if (source == Prefs.SOURCE_SURAH")[0]
check("the stream failed and no copy came: a saved voice, else the miss is kept for «الأذان» (never silent)",
      "int stand = !adhanFromCache && duaNow == null ? standIn(adhanVoice) : -1;" in fail and "prefs.setAdhanMissed(System.currentTimeMillis());" in fail
      and fail.index("standIn(adhanVoice)") < fail.index("setAdhanMissed") < fail.index("finishAdhan();"))
check("the stand-in is read from the saved list the check wrote, its one file looked at again", "AdhanKeep.parseSaved(prefs.adhanSaved(), Adhan.VOICES)" in ms and "return v > 0 && Adhan.isCached(this, v) ? v : -1;" in ms)
check("«الأذان» says under the muezzin: saved, not yet, or the last adhan missed (prefs only)", "Adhan.label(playVoice) + Adhan.keepNote(prefs, OwnAdhan.isOwn(playVoice))" in main
      and "File" not in ad.split("static String keepNote(Prefs prefs, boolean own) {")[1].split("\n    }\n")[0])
for ar in ["محفوظ على التلفزيون", "غير محفوظ على التلفزيون بعد، نحمّله أول ما يتوفر الإنترنت", "آخر أذان ما اشتغل لأنه ما كان محفوظًا على التلفزيون وما كان فيه إنترنت"]:
    check("English entry: " + ar, ("\n" + ar + "\t") in en and "·" not in ar)

if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "AdhanKeep.java"), str(ROOT / "tests/AdhanKeepRun.java")], capture_output=True, text=True)
        check("AdhanKeep compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.AdhanKeepRun"], capture_output=True, text=True, timeout=60).stdout
            got = dict(l.split("\t", 1) for l in out.splitlines() if "\t" in l)
            want = {
                "wanted_two": "3,10", "wanted_same": "3", "wanted_bad": "",
                "due_nothing_missing": "false", "due_never_tried": "true", "due_tried_10min_far": "false", "due_tried_61min_far": "true",
                "due_tried_10min_soon": "true", "due_tried_5min_soon": "false", "due_clock_back": "true",
                "parse": "4,10", "stand_other_first": "10", "stand_lowest": "4", "stand_never_self": "-1", "stand_none": "-1",
                "all_saved": "true false", "missed": "true false false false",
            }
            for k, v in want.items():
                check("AdhanKeep " + k, got.get(k) == v, got.get(k))
else:
    print("SKIP no javac")
check("the UI session: the muezzin and the place are joined with «، » («, » in English), never «—»", "static String label(int voice) { return name(voice) + sa.urtv.home.S.sep() + place(voice); }" in ad and " — " not in ad.split("static String label(int voice)")[1].split("\n")[0])
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
