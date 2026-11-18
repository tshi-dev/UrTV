#!/usr/bin/env python3
"""4.3.4 (a 4.3.3 regression): in the settings' «تحديث تلفزيوني», a step down with the remote SKIPPED «تحديث صفحة شاشة التوقف». Its version note
(note()) was made for the category's last row: when the row took the focus it called ScrollView.fullScroll(FOCUS_DOWN), which scrolls to the end
AND moves the focus to the lowest row. 4.3.3 put «النسخ التجريبية» (and, while on, «أرسل ملاحظة…») under it, so the focus landed there at once.
The note is now brought into view with requestRectangleOnScreen, which never moves the focus; nothing in the settings uses fullScroll.
Run: python3 tests/update_rows_focus_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

s = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
code = re.sub(r"//[^\n]*", "", s)   # what runs, not what the comments say
note = code.split("private void note(View last, String... lines) {")[1].split("\n    }\n")[0]
rows = s.split('View last = row("تحديث صفحة شاشة التوقف"')[1].split("restoreFocus(want, wasIndex, scrollY);")[0]

check("rows follow «تحديث صفحة شاشة التوقف» (why fullScroll skipped it)", 'row("النسخ التجريبية"' in rows)
check("the settings never call fullScroll (it moves the focus as it scrolls)", "fullScroll(" not in code)
check("the note is brought into view without a focus change", "t.requestRectangleOnScreen(" in note)
check("…only when its row takes the focus, after the row's own listener", "if (was != null) was.onFocusChange(v, has);" in note
      and "requestRectangleOnScreen" in note and note.index("if (!has) return;") < note.index("requestRectangleOnScreen"))
check("the note itself never takes the focus", "setFocusable(true)" not in note)
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
