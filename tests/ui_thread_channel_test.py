#!/usr/bin/env python3
"""4.3.3 hotfix (a TCL report: «التحديث الاخير الريموت فيه يعلق لتلفزيون tcl»): 4.3.3 put a cross-process query to the screensaver's ConfigProvider
(UpdateChannel.read) on the home's UI thread, twice in every minute tick (UpdateNotice.wanted, checkIfDue) and once at every return to the home
(BetaNotes.maybeAsk). On a TV that kills the screensaver's process (TCL) each query starts it again and holds the remote for seconds: the reason
the tick's own read moved off the UI thread (P3). Now the channel is kept by ConfigSnapshot's background read and the UI thread only takes the kept
word (UpdateChannel.cached). This test fails if any UI-thread path asks the screensaver again.
Run: python3 tests/ui_thread_channel_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return (H / p).read_text(encoding="utf-8")
uc, cs, un, bn, ra, main = read("phone/UpdateChannel.java"), read("ConfigSnapshot.java"), read("UpdateNotice.java"), read("BetaNotes.java"), read("ReportActivity.java"), read("MainActivity.java")
check("UpdateChannel.cached never queries (no content resolver in it)", "getContentResolver" not in uc.split("public static String cached() {")[1].split("\n    }\n")[0])
check("read() keeps what it learned for cached()", "String v = ask(c);\n        remember(v);" in uc)
check("ConfigSnapshot's background read keeps the channel", 'cur.getColumnIndex("update_channel")' in cs and "UpdateChannel.remember(cur.getString(uc))" in cs)
check("the tick's update pill: wanted() and checkIfDue() use the kept word", "UpdateChannel.read" not in un and un.count("UpdateChannel.cached()") == 2)
check("the return to the home (BetaNotes.maybeAsk, onResume) uses the kept word", "UpdateChannel.read" not in bn and "UpdateChannel.beta(" not in bn and "UpdateChannel.cached()" in bn)
check("the report screen uses the kept word", "UpdateChannel.read" not in ra and "UpdateChannel.cached()" in ra)
check("MainActivity itself never asks the channel", not re.search(r"UpdateChannel\.(read|beta)\(", main))
check("the minute tick still reads the screensaver's settings off the UI thread (P3)", 'final ConfigSnapshot got = ConfigSnapshot.read(app);' in main and '"home-config").start();' in main)
sys.exit(1 if failed else 0)
