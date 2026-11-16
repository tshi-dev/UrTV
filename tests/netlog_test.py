#!/usr/bin/env python3
"""G3 (4.3.3): the request log. NetLog counts every network request of the apps by host and writes one line (tag UrTVNet) per 10 minutes with the bytes from TrafficStats:
its rules on a plain JVM (tests/NetLogRun.java), then the wiring: the WebViews' requests (the screensaver page, the home's card page, the matches page), Net, the apps'
HttpURLConnection fetches, the two copies the same.

    python3 tests/netlog_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def text(p): return Path(p).read_text(encoding="utf-8")

for f in ("NetLog.java", "NetLogcat.java"):
    check(f"the home's {f} is the screensaver's, but for the package line", text(APP / f).split("\n", 1)[1] == text(HOME / f).split("\n", 1)[1])
check("the screensaver's page: each request it sends to the network is counted (not what PhotoCache serves from the disk)",
      re.search(r"PhotoCache\.serve\(app, r\.getUrl\(\)\.toString\(\)\);\s*if \(res != null\) return res;[^\n]*\n\s*NetLog\.count\(r\.getUrl\(\)\.toString\(\)\);", text(APP / "ScreenView.java")) is not None)
check("the home's card page and the matches page: counted before they go to the network",
      "NetLog.count(s);" in text(HOME / "HomeCard.java") and "NetLog.count(u);" in text(HOME / "MatchesActivity.java"))
check("Net counts each call, in both apps", all("NetLog.count(url);" in text(d / "Net.java") for d in (APP, HOME)))
missing = []
for d in (APP, HOME):
    for p in d.rglob("*.java"):
        s = text(p)
        for m in re.finditer(r"(\w+) = \([\w.]*HttpURLConnection\) new [\w.]*URL\([^\n]*\.openConnection\(\);[^\n]*\n([^\n]*)", s):
            if "NetLog.count(String.valueOf(" + m.group(1) + ".getURL()))" not in m.group(2) and p.name not in ("MirrorInstall.java", "SpeedTest.java", "FamilyDnsAndroid.java", "SetupApks.java", "SetupHost.java", "PhoneUpdater.java", "StreamUrl.java"):
                missing.append(p.name)
check("the apps' own HttpURLConnection fetches are counted (the speed test, the setup's and updater's downloads and the radio stream excepted)", not missing, missing)
check("the loggers are set where each app starts (the screensaver's Prefs, the home's Application)",
      "NetLog.logger = new NetLogcat();" in text(APP / "Prefs.java") and "NetLog.logger = new NetLogcat();" in text(HOME / "HomeApp.java"))
check("the line has the bytes from TrafficStats, tag UrTVNet", "TrafficStats.getUidRxBytes(uid)" in text(APP / "NetLogcat.java") and '"UrTVNet"' in text(APP / "NetLogcat.java"))

if not shutil.which("javac"):
    print("SKIP the JVM run: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    c = subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-d", tmp, str(APP / "NetLog.java"), str(ROOT / "tests/NetLogRun.java")], capture_output=True, text=True)
    check("NetLog compiles on a plain JVM", c.returncode == 0, c.stderr[-500:])
    if c.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.NetLogRun"], capture_output=True, text=True, timeout=60)
        for line in r.stdout.splitlines():
            if line.startswith(("PASS", "FAIL")):
                print(line)
                if line.startswith("FAIL"): failed += 1
        check("the JVM run ended", r.returncode == 0 or "FAIL" in r.stdout, r.stderr[-500:])
sys.exit(1 if failed else 0)
