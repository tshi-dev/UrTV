#!/usr/bin/env python3
"""The settings' lists with «حفظ» on a TV (a user: «الدوريات كاملة» had no way to save): the list of 16 leagues was taller than the
screen and pushed «حفظ» and «إلغاء» below it, out of reach of the remote. Every list of choices with «حفظ» now goes through Dlg.multi:
the list's height is capped so the buttons stay on screen (DialogFit, run for real on a JVM by tests/DialogFitRun.java), DOWN on its
last row goes to «حفظ», UP from a button goes back to the list, and Back saves as «حفظ» does (only «إلغاء» leaves without saving).
The wiring is read from SettingsActivity's source. The JVM part needs javac.   Run: python3 tests/dialog_fit_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
def fn(sig): return st[st.index(sig):].split("\n    }\n")[0]

# every dialog with a list of choices (setMultiChoiceItems) is a Dlg.multi one, and none has its own «حفظ» or «إلغاء» any more
dialogs = {}
for m in re.finditer(r"\n    private void (\w+)\([^)]*\) \{", st):   # each method by where it starts (pickMany has two)
    body = st[m.start() + 1:].split("\n    }\n")[0]
    if "setMultiChoiceItems(" in body: dialogs[m.group(1)] = body
check("the source has the lists with «حفظ» (the test reads what it should)",
      {"manageTeams", "pickMany", "pickLeagues", "manageShows", "pickMarkets"} <= set(dialogs), sorted(dialogs))
for name, body in dialogs.items():
    check(f"{name}: made with Dlg.multi (its list fits the screen, DOWN reaches «حفظ», Back saves)", "new Dlg(this).multi(() -> {" in body, name)
    check(f"{name}: no «حفظ» or «إلغاء» of its own besides multi's", 'setPositiveButton("حفظ"' not in body and 'setNegativeButton("إلغاء"' not in body, name)
check("every setMultiChoiceItems of the settings is in one of those", st.count("setMultiChoiceItems(") == sum(b.count("setMultiChoiceItems(") for b in dialogs.values()))
leagues = dialogs.get("pickLeagues", "")
check("«الدوريات كاملة» saves the leagues ticked (and keeps its limit of Prefs.MAX_LEAGUES while ticking)",
      'prefs.setList("leagues", v);' in leagues and "checked && n > Prefs.MAX_LEAGUES" in leagues
      and leagues.index(".multi(") < leagues.index('prefs.setList("leagues"') < leagues.index(".setTitle("))

dlg = st[st.index("private static final class Dlg"):].split("\n    }\n")[0]
multi = dlg[dlg.index("Dlg multi(final Runnable onSave)"):].split("\n        }\n")[0]
check("multi: «حفظ» runs the save, «إلغاء» does nothing, Back (cancel) runs the save too",
      'setPositiveButton("حفظ", (d, w) -> onSave.run());' in multi and 'setNegativeButton("إلغاء", null);' in multi
      and "setOnCancelListener(d -> onSave.run());" in multi)
check("multi: once shown, the dialog is dressed and then its list fitted", "owner.dress(d, raw);" in dlg and "if (save != null) owner.fitList(d);" in dlg
      and dlg.index("owner.dress(d, raw);") < dlg.index("owner.fitList(d);"))
fit = fn("void fitList(final AlertDialog d)")
check("fitList: DOWN on the last row (DialogFit.downToSave) gives «حفظ» the focus, and nothing else is taken from the list",
      "KEYCODE_DPAD_DOWN" in fit and "ACTION_DOWN" in fit and "DialogFit.downToSave(lv.getSelectedItemPosition(), lv.getCount())" in fit
      and "save.requestFocus();" in fit and "return false;" in fit)
check("fitList: UP from each button goes back to the list", "b.setNextFocusUpId(lv.getId());" in fit and "BUTTON_NEUTRAL" in fit)
check("fitList: the list's rows are measured at its width and its height set by DialogFit.listHeight (only when they do not fit)",
      "DialogFit.listHeight(content, dm.heightPixels, dm.density)" in fit and "row.measure(across, free);" in fit
      and "if (h < 0) return;" in fit and "lp.height = h;" in fit and "lv.setLayoutParams(lp);" in fit)
check("fitList: a dialog with no rows (no team followed yet) is left as it is", "if (lv == null || save == null) return;" in fit)

if not shutil.which("javac"):
    print("SKIP dialog fit: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "DialogFit.java"), str(ROOT / "tests/DialogFitRun.java")], check=True)
    run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.DialogFitRun"], capture_output=True)
out = run.stdout.decode("utf-8")
print(out, end="")
sys.exit(1 if failed or run.returncode != 0 or "FAIL" in out else 0)
