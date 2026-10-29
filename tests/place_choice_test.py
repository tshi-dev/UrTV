#!/usr/bin/env python3
"""The adhan alarm's place when the screensaver cannot be read (the owner's TCL kills its process and refuses to start it from the background
while another app starts: «callee doesn't have OP_ASSOC_START_permission», «Failed to find provider info»). A failed read must keep the last good
place, so the alarm is never set for the bundled one (Riyadh); only an install that never read anything uses the bundled place. The pure choice
(PlaceChoice/PlaceFragment on a plain JVM), then source checks of the wiring.
Needs javac for the Java cases. Run: python3 tests/place_choice_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


T = "android/launcher/src/main/java/sa/urtv/home/tilawah/"
H = "android/launcher/src/main/java/sa/urtv/home/"
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    srcs = [str(ROOT / T / n) for n in ("PlaceChoice.java", "PlaceFragment.java", "PlaceMemory.java", "SchedIdentity.java", "PrayerMath.java", "PrayerAdjust.java")] + [str(ROOT / H / "LastGood.java"), str(ROOT / "tests/PlaceChoiceRun.java"), str(ROOT / "tests/PlaceMemoryRun.java")]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + srcs, check=True, capture_output=True)
    for cls in ("PlaceChoiceRun", "PlaceMemoryRun"):
        out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah." + cls], capture_output=True, text=True).stdout
        for line in out.splitlines():
            if line.startswith(("PASS ", "FAIL ")):
                print(line)
                failed += line.startswith("FAIL ")
        check(f"every {cls} case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

snap0 = text(H + "ConfigSnapshot.java")
place = text(T + "Place.java")
check("Place: every read goes through the memory with the ticket taken BEFORE its read started, a failed read gets the remembered place, the bundled one only when there is none",
      "long ticket = PlaceTicket.next();   // taken before the read starts" in place and "MEMORY.resolve(store(c), fragment, ticket)" in place and "if (pick.fragment == null) return bundled(c);" in place
      and "return get(c, fresh, ticket);" in place and "static Place get(Context c, String fragment, long ticket) {" in place and "TICKETS.incrementAndGet" not in place and "PlaceTicket.next()" not in place.replace("long ticket = PlaceTicket.next();", ""))
check("the last good fragment is kept in this app's own preferences (loaded once, written only when it changed, only for a newer real answer)",
      'getSharedPreferences("place", Context.MODE_PRIVATE)' in place and "if (!fresh.equals(lastGood)) { lastGood = fresh; store.save(fresh); }" in text(T + "PlaceMemory.java") and "ticket >= accepted" in text(T + "PlaceMemory.java"))
check("a read that failed and used the last good place says so once per streak (tag UrTVAdhan, fixed words, no place)",
      'Log.i("UrTVAdhan", "place read failed: the last good place is used")' in place and "MEMORY.streakStarted(PlaceFragment.parse(fragment) == null, pick.remembered)" in place and "UrTVAdhan" not in text(T + "AdhanSync.java"))
check("the bundled place is marked, and its identity is '' (no real place matches it)", "this.bundled = bundled;" in place and "SchedIdentity.of(bundled," in place and "new int[PrayerAdjust.COUNT], null, true)" in place)
sync = text(T + "AdhanSync.java")
on = sync[sync.index("public static void onConfig"):sync.index("/** The shell asks")]
check("the ticket travels with the settings read: ConfigSnapshot takes it before the provider is asked, a cached or old copy keeps it, and every place caller passes placeTicket() with placeFragment()",
      "long ticket = sa.urtv.home.tilawah.PlaceTicket.next();   // before the provider is asked" in snap0 and "appearance, watchNextOn, look, true, placeTicket)" in snap0 and "long placeTicket() { return placeTicket; }" in snap0
      and all(all(c in text(H + f) for c in cs) for f, cs in {"AdhanBar.java": ["snap.placeFragment(), snap.placeTicket()"], "Greeting.java": ["snap.placeFragment(), snap.placeTicket()"], "MainActivity.java": ["cfg.placeFragment(), cfg.placeTicket()"]}.items())
      and "public static DayParts now(Context c, String fragment, long ticket)" in text(T + "DayParts.java") and "onConfig(Context c, String fragment, long ticket)" in text(T + "AdhanSync.java"))
check("the scheduler runs one at a time", "static synchronized void schedule(Context c, Place place)" in text(T + "AdhanScheduler.java"))
check("AdhanSync.onConfig: a null fragment (failed read or an old copy) decides nothing about the alarm; a real one is compared with what the alarm was really set from",
      "if (fragment == null) return;" in on and on.index("if (fragment == null) return;") < on.index("SchedIdentity.due(") and on.index("SchedIdentity.due(") < on.index("AdhanScheduler.schedule(c, p)") and "sched_identity" in on and "putString" not in sync)
check("the reschedule from the shell and the phone sets the alarm from the place it just read", "AdhanScheduler.schedule(c, Place.get(c));" in sync)
sch = text(T + "AdhanScheduler.java")
check("AdhanScheduler.schedule(c, place) is the ONE writer of sched_identity (it records the place it is set from, bundled or not), whoever calls it",
      sch.count('putString("sched_identity", place.identity())') == 1 and "sched_identity" not in sync.replace('getString("sched_identity"', "") and sch.index('putString("sched_identity"') < sch.index("cancelLegacy(c, am);")
      and "static void schedule(Context c) { schedule(c, Place.get(c)); }" in sch and "PrayerTimes.nextAlarm(c, place)" in sch and "static Next nextAlarm(Context c, Place place)" in text(T + "PrayerTimes.java"))
sch_users = [f.name for f in (ROOT / T).glob("*.java") if "sched_identity" in f.read_text(encoding="utf-8")]
check("sched_identity is read in AdhanSync and written in AdhanScheduler only", sorted(sch_users) == ["AdhanScheduler.java", "AdhanSync.java"], sch_users)
snap = text(H + "ConfigSnapshot.java")
snap0 = snap
check("ConfigSnapshot: a failed read hands out the last good values marked stale (not the defaults); a fresh answer is not stale",
      "LAST.read(" in snap and "return r.stale ? r.value.asStale() : r.value;" in snap and "String placeFragment() { return stale ? null : fragment; }" in snap)
callers = {"AdhanBar.java": ["DayParts.now(c, snap.placeFragment(), snap.placeTicket())"], "Greeting.java": ["DayParts.now(context, snap.placeFragment(), snap.placeTicket())"], "MainActivity.java": ["DayParts.now(this, cfg.placeFragment(), cfg.placeTicket())", "AdhanSync.onConfig(this, cfg.placeFragment(), cfg.placeTicket())"]}
check("every caller that turns the home's settings into a PLACE passes placeFragment() (null for an old copy): the alarm sync, the day parts of the bar, the greeting and the sun label",
      all(all(c in text(H + f) for c in cs) for f, cs in callers.items()), "")
bad = []
for f in (ROOT / H).rglob("*.java"):
    for n, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"(DayParts\.now|AdhanSync\.onConfig|Place\.get)\([^;]*\b(snap|cfg|s)\.fragment\b", line):
            bad.append(f"{f.name}:{n}")
check("no place is made from a snapshot's raw `fragment` (only placeFragment())", not bad, bad)
fa = text(T + "AdhanReceiver.java")
check("the alarm receiver still sets the next alarm through the scheduler (the remembered place when the read fails)", fa.count("AdhanScheduler.schedule(context)") == 2)
for n in ("PlaceFragment", "PlaceChoice", "PlaceMemory", "SchedIdentity", "PlaceTicket"):
    check(f"{n} is pure (no Android in it)", "android." not in text(T + n + ".java"))
check("LastGood is pure", "android." not in text(H + "LastGood.java"))
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
