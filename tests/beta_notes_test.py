#!/usr/bin/env python3
"""4.3.3 beta notes: on the beta channel only, the home asks ONCE for a note, after a beta update («انت على النسخة التجريبية…») and after a crash («صار خطأ،
تبي ترسل تقرير؟», the class name only); both open the existing «أرسل تقرير». Rule in tests/BetaNotesRun.java, wiring read from the source.
Run: python3 tests/beta_notes_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


bn, rule, app, main, ra, en = read(HOME / "BetaNotes.java"), read(HOME / "BetaNotesRule.java"), read(HOME / "HomeApp.java"), read(HOME / "MainActivity.java"), read(HOME / "ReportActivity.java"), read(ROOT / "i18n/en/home.tsv")
check("no Android in the rule", "import android" not in rule)
check("the crash hook is installed with the process and keeps the old handler", "BetaNotes.hookCrashes(this)" in app and "prev.uncaughtException(t, e)" in bn and "Thread.getDefaultUncaughtExceptionHandler()" in bn)
check("a crash keeps one thing: the class name, cleaned by Report.name (committed: the process is dying)", 'putString("crash", Report.name(e.getClass().getName())).commit()' in bn and "getMessage" not in bn and "getStackTrace" not in bn)
check("the question is asked from the home's onResume, not in kids mode, once (the note is removed as it is read)", "if (!Parental.state(this).kids) BetaNotes.maybeAsk(this);" in main and 'remove("crash")' in bn)
check("only on the beta channel: the rule is given the channel, and the build seen is kept only there", '"beta".equals(UpdateChannel.cached())' in bn and "if (beta) e.putLong(\"seen\", code)" in bn)
check("both buttons open the existing report screen; a crash hands over its class name", "new Intent(a, ReportActivity.class)" in bn and 'i.putExtra("error", error)' in bn and 'getStringExtra("error")' in ra)
for w in ("صار خطأ، تبي ترسل تقرير؟", "انت على النسخة التجريبية، إذا شفت شي غريب أرسل ملاحظة"):
    check("«" + w + "» is said through S.t and has its English", ('"' + w + '"') in bn and ("\n" + w + "\t") in en)
if not shutil.which("javac"):
    print("SKIP rule: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "Report.java"), str(HOME / "BetaNotesRule.java"), str(ROOT / "tests/BetaNotesRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BetaNotesRun"], capture_output=True, text=True).stdout
print(out.strip())
check("every case of BetaNotesRun ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
