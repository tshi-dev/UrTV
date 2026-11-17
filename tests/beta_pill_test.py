#!/usr/bin/env python3
"""4.3.3: the home's update pill (UpdateNotice) follows the chosen channel: apk-beta while «النسخ التجريبية» is on, and a number read from another channel counts
for nothing. Wiring read from the source.   Run: python3 tests/beta_pill_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


un = (ROOT / "android/launcher/src/main/java/sa/urtv/home/UpdateNotice.java").read_text(encoding="utf-8")
check("version.txt is read from the chosen channel's release, never the latest's fixed address", "UpdateRules.baseFor(channel) + \"version.txt\"" in un and "UpdateRules.BASE +" not in un)
check("the number is stored with its channel", 'putInt("newest", build).putString("channel", channel)' in un)
check("the pill uses only a number read on the current channel", "newestFor(c, UpdateChannel.read(c))" in un and 'channel.equals(sp.getString("channel", "latest")) ? sp.getInt("newest", 0) : 0' in un)
check("a changed channel is asked again at once, not after 12 hours", "if (sameChannel && !UpdateNoticeRule.due(" in un)
sys.exit(1 if failed else 0)
