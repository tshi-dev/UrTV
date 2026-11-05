#!/usr/bin/env python3
"""A radiojar station is read by us (tester step 32): LiveFeed against a local server (tests/LiveFeedRun.java), then the wiring in MusicService.

    python3 tests/live_feed_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "StreamUrl.java"), str(T / "LiveFeed.java"), str(T / "LiveProxy.java"), str(ROOT / "tests/LiveFeedRun.java")], capture_output=True, text=True)
    check("it compiles", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.LiveFeedRun"], capture_output=True, text=True, timeout=120).stdout
        for line in out.splitlines():
            if line.startswith(("PASS ", "FAIL ")):
                print(line)
                failed += line.startswith("FAIL ")
        check("every LiveFeed case ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP the Java cases: no javac")

svc = (T / "MusicService.java").read_text(encoding="utf-8")
check("a radiojar station is served to the player as an http stream on 127.0.0.1 by LiveProxy (a MediaDataSource stalled the TV's sniffer); any other address as before",
      "StreamUrl.cleartextAllowed(hostOf(url))" in svc and "liveProxy = LiveProxy.start(feed);" in svc and "player.setDataSource(liveProxy.url());" in svc and "player.setDataSource(url);" in svc
      and "if (liveProxy != null) { liveProxy.close(); liveProxy = null; }" in svc and not (T / "LiveSource.java").exists() and "MediaDataSource" not in svc)
nsc = (ROOT / "android/launcher/src/main/res/xml/network_security_config.xml").read_text(encoding="utf-8")
check("cleartext is allowed for radiojar.com and for 127.0.0.1 (the proxy) only", '<domain includeSubdomains="false">127.0.0.1</domain>' in nsc and nsc.count("<domain ") == 2)
sys.exit(1 if failed else 0)
