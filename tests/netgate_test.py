#!/usr/bin/env python3
"""The network rule: the home screen's fetches run only while the home is in front and the display is on (NetGate.java, pure cases on
tests/netgate_cases.tsv), and the places that fetch ask the gate (source checks). The adhan's alarm itself never asks it.
Needs javac for the pure cases (skipped with a note without).   Run: python3 tests/netgate_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, why=""):
    global failed
    print(("PASS " if ok else "FAIL ") + "netgate " + name + ("" if ok else "  " + str(why)))
    failed += not ok


def src(p):
    return p.read_text(encoding="utf-8")


def method(text, head):
    """the body of the method that starts at `head` (to the closing brace at the same indent)"""
    i = text.index(head)
    indent = re.search(r"\n( *)\S", "\n" + text[text.rfind("\n", 0, i) + 1:i + 1]).group(1)
    j = text.index("\n" + indent + "}", i)
    return text[i:j]


# ---- the pure rule
if shutil.which("javac"):
    cases = [l.split("\t") for l in (ROOT / "tests/netgate_cases.tsv").read_text(encoding="utf-8").split("\n")
             if l and not l.startswith("#")]
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(H / "NetGate.java"), str(ROOT / "tests/NetGateRun.java")], check=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.NetGateRun", str(ROOT / "tests/netgate_cases.tsv")],
                             check=True, capture_output=True).stdout.decode().split("\n")
    n = {"allowed": 2, "due": 5, "background": 1}
    for c, g in zip(cases, out):
        want = c[1 + n[c[0]]]
        check(f"{c[0]}: {c[2 + n[c[0]]]}", g == want, f"-> {g!r}, wanted {want!r}")
    check("one answer per case", len([g for g in out if g]) == len(cases))
else:
    print("SKIP netgate pure cases: no javac here")

gate = src(H / "NetGate.java")
check("NetGate has no Android import", "import android" not in gate)

# ---- the places that fetch
greeting = src(H / "Greeting.java")
check("Greeting: the weather fetch asks NetGate (front and display)", "NetGate.due(" in greeting and "NetGate.allowed(" in greeting
      and "NetGateAndroid.displayOn" in greeting)
check("Greeting: no fetch call outside that decision", greeting.count("fetch(s)") == 1)
main = src(H / "MainActivity.java")
check("MainActivity: Greeting is told whether the home is resumed", "new Greeting(this, greeting, weatherLine, () -> resumed)" in main)
check("MainActivity: resumed is volatile (the photo thread reads it)", "private volatile boolean resumed" in main)
nxt = method(main, "private void nextWallpaper()")
check("MainActivity: the photo list and downloads ask NetGate after the local photo is shown",
      nxt.index("showWallpaperIfCurrent(bm, gen, key);") < nxt.index("NetGate.allowed(resumed") < nxt.index("refreshPhotoIndex();"))
pt = src(H / "tilawah/PrayerTimes.java")
check("PrayerTimes.refresh: skipped with the display off", "NetGate.backgroundOk" in method(pt, "static void refresh(final Context c"))
check("PrayerTimes.refreshIfDue: a try is not counted with the display off",
      method(pt, "static void refreshIfDue(").index("NetGate.backgroundOk") < method(pt, "static void refreshIfDue(").index('"refresh_try"'))
check("PrayerTimes.refreshNow itself has no gate (the adhan alarm path and the tests call refresh)", "NetGate" not in method(pt, "static boolean refreshNow("))
ad = src(H / "tilawah/Adhan.java")
check("Adhan.prefetchChosen: skipped with the display off, remembered for the home's return (4.4 beta 3: the gate is in keepNow, which prefetchChosen hands off to)",
      "keepNow(app, ASKED)" in method(ad, "static void prefetchChosen(") and "NetGate.backgroundOk" in method(ad, "private static void keepNow(") and "skippedInStandby = true" in ad)
check("AdhanSync: a skipped prefetch is made when the home is back (once, no polling)", "Adhan.prefetchIfSkipped(c)" in src(H / "tilawah/AdhanSync.java"))
rec = src(H / "tilawah/AdhanReceiver.java")
check("AdhanReceiver: the alarm plays and reschedules first, whatever the network (refresh after, gated inside)",
      rec.index("AdhanScheduler.schedule(context);\n        PrayerTimes.refresh(context, null);\n    }") > rec.index("MusicService.adhan("))
sch = src(H / "tilawah/AdhanScheduler.java")
check("AdhanScheduler: setting the alarm never waits for the network", "PrayerTimes.nextAlarm(c)" in sch and "NetGate" not in sch)
card = src(H / "HomeCard.java")
check("HomeCard: pauses its page's timers when another app comes up", "web.pauseTimers()" in method(card, "void pause()"))
up = src(A / "Updater.java")
cid = method(up, "static void checkIfDue(")
check("Updater.checkIfDue: not with the display off, and before lastCheck is touched", "displayOn(app)" in cid and cid.index("displayOn(app)") < cid.index('"lastCheck"'))
job = src(A / "UpdateJob.java")
check("UpdateJob: still only wakes the process (no network)", "return false;" in method(job, "public boolean onStartJob(") and "Updater." not in method(job, "public boolean onStartJob("))
check("docs/NETWORK.md exists and is linked from the README", (ROOT / "docs/NETWORK.md").is_file() and "docs/NETWORK.md" in src(ROOT / "README.md"))
sys.exit(1 if failed else 0)
