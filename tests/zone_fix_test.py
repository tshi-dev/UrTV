#!/usr/bin/env python3
"""4.3 (issue #55): in Safi (Africa/Casablanca) the home's clock was an hour ahead of the page's: the TV's own tz data is older than the
WebView's, and Morocco has been on +00 for good since 2026-09-20 (tzdata 2026c+). Every native time of a city or of the TV now comes from
ZoneFix (a table made from a recent tz database for the zones whose rules changed, else the TV's data, moved to the page's word near the moment
the page told it): ZoneFixRun.java with fake TV data (+01, +02, +00), the page's word, Calendar both ways, Safi's prayer times and a reminder's
day step; and here, that nothing native reads a zone around it.
    python3 tests/zone_fix_test.py"""
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


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


strip = lambda t: re.sub(r"^package [a-z.]+;", "", t, flags=re.M).replace("(the launcher's is in sa.urtv.home)", "").replace("(the screensaver's is in sa.urtv.screensaver)", "")
check("both apps carry the same ZoneFix and ZoneTable", strip(read(H / "ZoneFix.java")) == strip(read(A / "ZoneFix.java"))
      and strip(read(H / "ZoneTable.java")) == strip(read(A / "ZoneTable.java")))
tab = read(H / "ZoneTable.java")
ver = re.search(r'VERSION = "([^"]+)"', tab).group(1)
check("the table is from tzdata 2026e or newer (Morocco's +00 from 2026-09-20)", ver >= "2026e" and "29831100:0" in tab.split('"Africa/Casablanca|')[1].split('"')[0], ver)
sysver = ""
try:
    sysver = Path("/usr/share/zoneinfo/tzdata.zi").read_text(encoding="utf-8").splitlines()[0].replace("# version", "").strip()
except OSError:
    pass
if sysver == ver:
    r = subprocess.run([sys.executable, "-I", str(ROOT / ".github/tools/zone_table.py"), "--check"], capture_output=True, text=True)
    check("ZoneTable is what .github/tools/zone_table.py makes from this tzdata", r.returncode == 0, r.stdout + r.stderr)
else:
    print(f"SKIP the table's remake: this machine has tzdata {sysver or 'none'}, the file {ver}")

# nothing native reads a city's or the TV's zone around ZoneFix
allowed = re.compile(r'TimeZone\.getTimeZone\("(UTC|America/New_York|Asia/Riyadh)"\)|getAvailableIDs')
bad = []
for root in (H, A):
    for p in root.rglob("*.java"):
        if p.name in ("ZoneFix.java", "ZoneIcu.java", "ZoneTable.java"):
            continue
        for i, line in enumerate(read(p).splitlines(), 1):
            code = line.split("//")[0]
            if re.search(r"\bTimeZone\.getTimeZone\(|\bTimeZone\.getDefault\(\)(?!\.getID\(\))|icu\.util\.TimeZone\.getTimeZone\(", code) and not allowed.search(code):
                if 'getID()) && !"GMT".equals(id)' in read(p).splitlines()[i] or "getTimeZone(id);" in code:   # the «unknown id» guards of Reminders and ReminderStore
                    continue
                bad.append(f"{p.relative_to(ROOT)}:{i}")
check("every native zone of a city or the TV goes through ZoneFix (the clock, dates, Hijri, prayers, reminders, matches, markets)", not bad, bad)
ma = read(H / "MainActivity.java")
check("the home's clock, both date lines and the Hijri day take ZoneFix's zone each tick", "gregFormat.setTimeZone(ZoneFix.zone(tz));" in ma
      and "hijriFormat.setTimeZone(ZoneIcu.of(tz));" in ma and "java.util.TimeZone zone = ZoneFix.zone(tz);" in ma and "Calendar.getInstance(ZoneIcu.of(tz)," in ma)
t = read(ROOT / "src/template.html")
check("the page tells the screensaver its offset for the city's zone, at the start and every hour, the screen only",
      "window.UrtvNative.zoneOffset(CONFIG.timezone, String(t), Math.round(tzOffsetMs(t) / 60000));" in t and "setInterval(reportZone, 60*60*1000);" in t
      and "if(CONFIG.widget || !(window.UrtvNative && window.UrtvNative.zoneOffset)) return;" in t)
for f in ("dist/tv.html", "dist/tv-en.html"):
    check(f"{f} is built with it", "zoneOffset(CONFIG.timezone" in read(ROOT / f))
br, cp, cs = read(A / "NowPlayingBridge.java"), read(A / "ConfigProvider.java"), read(H / "ConfigSnapshot.java")
check("the screensaver keeps the page's word only for the city's own zone, a moment within a day, a sane offset, and gives it to the home",
      "!zone.equals(p.cityTz()) || Math.abs(now - t) > 86_400_000L || minutes < -14 * 60 || minutes > 14 * 60" in br and '"page_zone"' in cp and "p.pageZone()" in cp
      and 'ZoneFix.report(cur.getString(pz));' in cs and "ZoneFix.report(pageZone());" in read(A / "Prefs.java"))
new = [H / "ZoneFix.java", H / "ZoneTable.java", H / "ZoneIcu.java", A / "ZoneFix.java", A / "ZoneTable.java", ROOT / ".github/tools/zone_table.py"]
check("the content rule: Western Sahara is never named (Africa/El_Aaiun is only an id)", not any(re.search(r"western\s+sahara|الصحراء الغربية", read(p), re.I) for p in new))

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        srcs = [H / "ZoneFix.java", H / "ZoneTable.java", H / "ReminderPlan.java", H / "tilawah/PrayerMath.java", ROOT / "tests/ZoneFixPeek.java", ROOT / "tests/ZoneFixRun.java"]
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(s) for s in srcs], capture_output=True, text=True)
        check("ZoneFix, PrayerMath and ReminderPlan compile on a plain JVM", r.returncode == 0, r.stderr[-500:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.ZoneFixRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"ZoneFix: every case ({len(lines)})", len(lines) == 34 and not bad, bad or out)
sys.exit(1 if failed else 0)
