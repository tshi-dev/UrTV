#!/usr/bin/env python3
"""A weak TV (a user report on 4.1: heavy lag after the update): Ram.low (Android's low-RAM flag, or under 2.5 GB) and what the home does less there:
the key filter off, the clock's settings read off the UI thread with a minute guard for Wi-Fi broadcasts, the pictures and the card page let go
while another app is in front, the markets' live mode off at first and at most every 60 s, and the page's #lowram=1 (slower timers, no seconds,
no transitions). The screensaver's city lookup is a map now (3,679 cities, asked at every minute). The rules on a JVM (tests/LowRamRun.java, small
Android stubs for Ram's Context reading), then the wiring in the sources.

    python3 tests/lowram_test.py
"""
import re, shutil, subprocess, sys, tempfile
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


STUBS = {
    "android/content/Context.java": "package android.content;\npublic abstract class Context { public static final String ACTIVITY_SERVICE = \"activity\";"
                                    " public abstract Context getApplicationContext(); public abstract Object getSystemService(String n); }\n",
    "android/app/ActivityManager.java": "package android.app;\npublic class ActivityManager { public boolean isLowRamDevice() { return false; }"
                                        " public void getMemoryInfo(MemoryInfo m) { } public static class MemoryInfo { public long totalMem; } }\n",
}

if shutil.which("javac") and shutil.which("java"):
    tmp = Path(tempfile.mkdtemp())
    for rel, body in STUBS.items():
        (tmp / "src" / rel).parent.mkdir(parents=True, exist_ok=True)
        (tmp / "src" / rel).write_text(body)
    srcs = [str(tmp / "src" / r) for r in STUBS] + [str(HOME / f) for f in ("Ram.java", "ClockTick.java", "SettingsKey.java", "MarketData.java", "DigitsText.java")] + [str(ROOT / "tests/LowRamRun.java")]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(tmp / "out"), *srcs], capture_output=True, text=True)
    check("the rules compile", r.returncode == 0, r.stderr[-800:])
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", str(tmp / "out"), "sa.urtv.home.LowRamRun"], capture_output=True, text=True, timeout=60).stdout
        for line in out.splitlines():
            if line.startswith(("PASS ", "FAIL ")):
                print(line); failed += line.startswith("FAIL ")
        check("every rule ran", "ALL PASS" in out, out[-300:])
    # the screensaver's city map against the old scan, on every built-in city
    cj = tmp / "cities"; (cj / "src/sa/urtv/screensaver").mkdir(parents=True)
    (cj / "src/sa/urtv/screensaver/CitiesRun.java").write_text('''package sa.urtv.screensaver;
public class CitiesRun { public static void main(String[] a) {
  int n = 0, bad = 0;
  for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) {
    String[] scan = null;
    outer: for (Cities.Region r2 : Cities.REGIONS) for (String[] x : r2.cities) if (x[0].equals(c[0]) && x[1].equals(c[1]) && x[2].equals(c[2])) { scan = x; break outer; }
    if (Cities.find(c[0], c[1], c[2]) != scan) bad++;
    n++;
  }
  boolean none = Cities.find("لا مدينة", "1", "2") == null && Cities.find(null, "1", "2") == null;
  System.out.println("cities=" + n + " bad=" + bad + " none=" + none);
} }
''')
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(cj / "out"), str(SAVER / "Cities.java"), str(cj / "src/sa/urtv/screensaver/CitiesRun.java")], capture_output=True, text=True)
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", str(cj / "out"), "sa.urtv.screensaver.CitiesRun"], capture_output=True, text=True, timeout=120).stdout.strip()
        m = re.match(r"cities=(\d+) bad=(\d+) none=(\w+)", out)
        check("Cities.find (the map) gives exactly what the old scan gave for every built-in city, and null for none", m and int(m.group(1)) > 3000 and m.group(2) == "0" and m.group(3) == "true", out)
    else:
        check("Cities.java compiles alone", False, r.stderr[-500:])
else:
    print("SKIP the JVM cases: no javac")

ts = text(HOME / "TypeService.java")
check("TypeService asks the rule with Ram.low, and skips its package query on a weak TV", "SettingsKey.filterWanted(front, stockHome, lowRam)" in ts and "if (!front && !lowRam) stockHome = stockHomeOn(this);" in ts)
ma = text(HOME / "MainActivity.java")
tick = ma[ma.index("private final BroadcastReceiver tick"):ma.index("private long clockDrawnAt;")]
check("the clock's tick: the minute guard for Wi-Fi broadcasts, the settings read on clockReads, then the redraw on the UI thread only while in front",
      "ClockTick.redraw(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(i.getAction()), clockDrawnAt, System.currentTimeMillis())" in tick
      and tick.index("clockReads.execute") < tick.index("ConfigSnapshot.read(app)") < tick.index("runOnUiThread") < tick.index("updateClock();")
      and "if (isDestroyed() || !resumed) return;" in tick and "clockDrawnAt = ms;" in ma and "clockReads.shutdownNow();" in ma)
trim = ma[ma.index("public void onTrimMemory(int level)"):ma.index("private boolean wallTrimmed;")]
check("onTrimMemory: only on a weak TV, only with another app in front (UI_HIDDEN or more): the card page and both photos let go; onResume shows a photo at once",
      "if (level < TRIM_MEMORY_UI_HIDDEN || resumed || !Ram.low(this)) return;" in trim and "homeCard.destroy();" in trim and "for (ImageView v : new ImageView[] {wallFront, wallBack}) if (v != null) v.setImageDrawable(null);" in trim
      and "if (wallTrimmed || wallIndex < 0" in ma)
mk = text(HOME / "MarketsActivity.java")
check("markets: «عادي» at first on a weak TV, and the live pace from liveDelay(…, Ram.low(this))", "if (Ram.low(this)) liveOn = false;" in mk
      and mk.count("MarketData.liveDelay(") == mk.count(", Ram.low(this)))") == 2)
pf = text(SAVER / "Prefs.java")
check("the screensaver sends #lowram=1 on a weak TV (the same rule: the flag, or under 2560 MB)", '(lowRam(ctx) ? "&lowram=1" : "")' in pf and "mb > 0 && mb < 2560" in pf
      and "isLowRamDevice()" in pf and "LOW_MB = 2560" in text(HOME / "Ram.java"))
check("a city saved before 4.1 gets its country found once and kept; cityTz uses the map, not a scan", 'sp.edit().putString("cityCountry", code).apply();' in pf
      and "for (Cities.Region r : Cities.REGIONS)" not in pf[pf.index("String cityTz()"):pf.index("String cityTz()") + 600])
for name in ("src/template.html", "dist/tv.html", "dist/tv-en.html"):
    t = text(ROOT / name)
    check(f"{name}: #lowram=1: no seconds, the markets every 15 min, the adhan check every 15 s, reminders every 5 s, no transitions; else as before",
          "lowram: _Q.get('lowram') === '1'," in t and "seconds: _Q.get('seconds') === '1' && _Q.get('lowram') !== '1'," in t
          and t.count("(CONFIG.lowram ? 15 : 5)*60*1000") == 2 and "setInterval(adhanLiveTick, CONFIG.lowram ? 15000 : 3000);" in t
          and "setInterval(remDraw, CONFIG.lowram ? 5000 : 1000)" in t and "html.lowram *, html.lowram *::before, html.lowram *::after { transition: none !important; }" in t
          and t.count("if(CONFIG.lowram) document.documentElement.classList.add('lowram');") == 2)
sys.exit(1 if failed else 0)
