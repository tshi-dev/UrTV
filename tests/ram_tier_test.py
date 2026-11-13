#!/usr/bin/env python3
"""The three memory tiers (step 47): LOW (the real low-RAM flag, or under 2000 MB) does every cut as before; MID (2000 to under 2600 MB, the owner's TCL at
2394) only the cheap memory cuts; NORMAL (2600 MB and up, or an unknown total) none. One place decides in each app (Ram.tier in the home, Prefs.ramTier in the
screensaver app); the numbers are the same in both, the page gets #lowram=1 or #midram=1, and a test switch (saver.sh / dock.sh ramtier) forces a tier.
The JVM boundary cases of the home's rule are in LowRamRun (run by lowram_test.py); here the saver app's copy is read from source and run in Python.

    python3 tests/ram_tier_test.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


ram, pf = text(HOME / "Ram.java"), text(SAVER / "Prefs.java")
hm = re.search(r"LOW_MB = (\d+), MID_MB = (\d+);", ram)
sm = re.search(r"LOW_MB = (\d+), MID_MB = (\d+);", pf)
check("both apps use the same two thresholds: 2000 MB and 2600 MB", hm and sm and hm.groups() == sm.groups() == ("2000", "2600"), (hm and hm.groups(), sm and sm.groups()))
LOW_MB, MID_MB = int(sm.group(1)), int(sm.group(2))


def tier(flag, mb):   # the saver app's rule as written in Prefs.ramTier(boolean, long)
    if flag or (mb > 0 and mb < LOW_MB):
        return 0
    return 1 if mb > 0 and mb < MID_MB else 2


check("the saver app's rule is written as the model here (flag or under LOW_MB = LOW; under MID_MB = MID; else NORMAL)",
      "if (lowRamFlag || (mb > 0 && mb < LOW_MB)) return RAM_LOW;" in pf and "return mb > 0 && mb < MID_MB ? RAM_MID : RAM_NORMAL;" in pf)
check("boundaries: 1999 LOW, 2000 MID, 2394 MID (the TCL), 2599 MID, 2600 NORMAL, 0 (unknown) NORMAL, the flag LOW at any size; 1680 and 1830 LOW",
      [tier(False, m) for m in (1999, 2000, 2394, 2599, 2600, 0, 1680, 1830, 2800)] == [0, 1, 1, 1, 2, 2, 0, 0, 2] and tier(True, 8192) == 0)
check("one place: no other MemTotal check in either app (only Ram.java and Prefs.ramTier look at the memory)",
      all("totalMem" not in text(f) for f in list(HOME.rglob("*.java")) if f.name != "Ram.java")
      and not re.search(r"< ?2560|2560", " ".join(text(f) for f in list(HOME.rglob("*.java")) + list(SAVER.glob("*.java")))))
check("what LOW keeps doing only for LOW: the key filter (TypeService), the markets' «عادي» start, the page's #lowram=1 (no seconds, slow timers, no transitions, one photo layer)",
      "lowRam = Ram.low(this)" in text(HOME / "TypeService.java") and "if (Ram.low(this)) liveOn = false;" in text(HOME / "MarketsActivity.java")
      and '(lowRam(ctx) ? "&lowram=1" : midRam(ctx) ? "&midram=1" : "")' in pf)
check("what MID keeps (the cheap memory cuts): drop-on-trim and the card page's idle drop (Ram.tight), photos at 1280, own photos at most 25, the markets a little slower (45 s live, 10 min page)",
      "!Ram.trimNow(Ram.tier(this), level)) return;" in text(HOME / "MainActivity.java") and "Ram.tight(activity)) idle.postDelayed" in text(HOME / "HomeCard.java")
      and "Prefs.tightRam(c) ? PhotoShrink.toSize(data, LOW_PX)" in text(SAVER / "PhotoCache.java")
      and "MAX = 50, MAX_MID = 25, MAX_LOW = 10" in text(SAVER / "Own.java") and "Prefs.lowRam(c) ? MAX_LOW : Prefs.midRam(c) ? MAX_MID : MAX" in text(SAVER / "Own.java")
      and "Math.max(45_000, liveDelay(fails))" in text(HOME / "MarketData.java") and "(CONFIG.lowram ? 15 : CONFIG.midram ? 10 : 5)*60*1000" in text(ROOT / "src/template.html"))
tpl = text(ROOT / "src/template.html")
check("what MID gives back: seconds, the normal timers, transitions and the second photo layer follow only #lowram=1 (never #midram=1)",
      "midram" not in tpl.replace("midram: _Q.get('midram') === '1',", "").replace("CONFIG.midram ? 10 : 5", "")
      and "seconds: _Q.get('seconds') === '1' && _Q.get('lowram') !== '1'," in tpl)
check("the 1 fps settle and «animate only on change» stay for everyone (the settle is drawn in 60 steps whatever the tier)", ".photo.settle{animation:settle 60s steps(60,end) both}" in tpl)
check("the test switch: saver.sh ramtier (SaverReceiver ramtier, shown by `show`), dock.sh ramtier (RAM_TIER in the manifest and DockReceiver); a forced tier counts from the next start",
      "ramtier)" in text(ROOT / "optimize/saver.sh") and 'getStringExtra("ramtier")' in text(SAVER / "SaverReceiver.java") and '"\\nramtier: "' in text(SAVER / "SaverReceiver.java")
      and "ramtier:2)" in text(ROOT / "optimize/dock.sh") and "sa.urtv.home.RAM_TIER" in text(ROOT / "android/launcher/src/main/AndroidManifest.xml")
      and 'case "sa.urtv.home.RAM_TIER"' in text(HOME / "DockReceiver.java") and "Ram.force(c, tn)" in text(HOME / "DockReceiver.java"))
check("the settings doc has the line", "ramtier" in text(ROOT / "docs/SETTINGS.md"))
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
