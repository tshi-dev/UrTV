#!/usr/bin/env python3
"""The audit's missing test (X, 27 Nov «دعاء من فلاشة في يوم ووقت»): a dua's sound picked from a USB stick, the whole path through «صوتياتي» in pick mode:
the dua's «صوت» row opens AudioActivity with EXTRA_PICK -> the first page still offers «أضف من فلاشة أو التنزيلات» (not hidden by the pick mode) -> the copied file is handed
back as the result (hand: RESULT_OK + "id") instead of asking what it is for -> DuaActivity stores it as the dua's sound «a:<id>» and keeps the dua."""
import re, sys
from pathlib import Path
T = Path(__file__).resolve().parents[1] / "android/launcher/src/main/java/sa/urtv/home/tilawah"
dua = (T / "DuaActivity.java").read_text(encoding="utf-8"); au = (T / "AudioActivity.java").read_text(encoding="utf-8")
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
check("the dua's sound is picked through «صوتياتي» in pick mode (EXTRA_PICK, a result)", "startActivityForResult(new Intent(this, AudioActivity.class).putExtra(AudioActivity.EXTRA_PICK, true), PICK_SOUND)" in dua)
first = au[au.index("@Override Page first()"):]
first = first[:first.index("@Override String hintText()")]
add_row = first.index('out.add(Row.of(S.t("أضف من فلاشة أو التنزيلات")')
pick_guard = first.index("if (!pick) {")
check("the stick and Downloads row is on the first page in pick mode too (it comes before, and outside, the `if (!pick)` block of the capacity rows)", add_row < pick_guard)
copy = au[au.index("private void copy(File f)"):]
copy = copy[:copy.index('"audio-copy").start()')]
check("a file copied in pick mode is handed back at once, before the question of what it is for", "if (pick) { hand(a.item); return; }" in copy and copy.index("if (pick) { hand(a.item); return; }") < copy.index("ask("))
check("hand() returns RESULT_OK with the file's id and closes the screen", 'setResult(RESULT_OK, new Intent().putExtra("id", it.id));' in au and "finish();" in au[au.index("private void hand("):au.index("private void hand(") + 160])
res = dua[dua.index("protected void onActivityResult"):]
res = res[:res.index("\n    }\n")]
check("the dua stores the picked file as its sound «a:<id>», takes its name when it has none, and keeps it", 'data.getIntExtra("id", -1)' in res and 'edit.sound = "a:" + it.id;' in res and "edit.name = it.name" in res and "keep();" in res)
check("a result that is not RESULT_OK, or a file that is gone, changes nothing", "result != RESULT_OK" in res and "if (it == null) return;" in res)
sys.exit(1 if failed else 0)
