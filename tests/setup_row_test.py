#!/usr/bin/env python3
"""4.3.1 «إعداد تلفزيوني» is a row of the settings (Home screen) that always opens the setup, even when UrTV is already the home (the nudge card and the one-shot reopen were the only ways in).
    python3 tests/setup_row_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
S = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: Path(p).read_text(encoding="utf-8")
st, hub, words = read(S / "SettingsActivity.java"), read(S / "SettingsHub.java"), read(S / "Words.java")
check("the row opens the setup whatever the home is (not `if (!isHome)`), and is listed in the hub and the words", 'row("إعداد تلفزيوني"' in st and "openSetup()" in st.split('row("إعداد تلفزيوني"')[1][:200]
      and '{"إعداد تلفزيوني", HOME}' in hub and 'S.n("إعداد تلفزيوني")' in words and 'S.n("شغّل الإعداد من جديد، أو تراجع عن تغييراته")' in words)
sys.exit(1 if failed else 0)
