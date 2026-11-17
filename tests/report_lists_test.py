#!/usr/bin/env python3
"""4.4 (reports, cause 10): apps that were already off before setup are never listed as our changes. The in-TV report (ReportLists, as optimize/share.py
writes it) names them apart; the ledgers (SetupEngine.ledgerText for the safe list and the community list, optimize/disable.sh for the computer) never
hold a package the backup says was off when the TV came to us, so «تراجع» / reset.sh never turns on what the maker had off.
Run: python3 tests/report_lists_test.py"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

rep, eng, com, dis = read(SETUP / "SetupReport.java"), read(SETUP / "SetupEngine.java"), read(SETUP / "SetupCommunity.java"), read(ROOT / "optimize/disable.sh")
check("the report's list lines come from ReportLists with the backup's «before»", "out.addAll(ReportLists.lines(e.getKey(), e.getValue(), installed, disabled, before));" in rep)
check("ReportLists has no Android in it", "import android" not in read(SETUP / "ReportLists.java"))
lt = eng.split("static String ledgerText(")[1].split("\n    }\n")[0]
check("the engine's ledger drops what the backup says was off before", 'SetupPrep.section(backup, "disabled packages")' in lt and "!before.contains(p)" in lt
      and 'store.put("disabled-by-us.txt", ledgerText(prev, add, store.get(SetupPrep.BACKUP)));' in eng)
check("the community list's ledger goes through the same rule", "if (LEDGER.equals(file)) { store.put(file, SetupEngine.ledgerText(" in com)
check("disable.sh's ledger drops them too", "before_off=$(sed -n '/^== disabled packages/,/^== /s/^package://p'" in dis and '<(printf \'%s\\n\' "$before_off")' in dis)

# disable.sh's remember(), run as it is in the script
with tempfile.TemporaryDirectory() as d:
    run = Path(d)
    (run / "backup.txt").write_text("$ echo '== disabled packages'; pm list packages -d\n== disabled packages\npackage:maker.off\n$ echo '== home'\n== home\n", encoding="utf-8")
    body = dis.split('prev=""; [ ! -f "$LEDGER" ]')[0]
    fn = [l for l in dis.splitlines() if l.startswith(("before_off=", "remember()"))]
    script = 'RUN="%s"; LEDGER="$RUN/disabled-by-us.txt"; prev="old.one"\n%s\nremember "$(printf "a.ours\\nmaker.off\\n")"\ncat "$LEDGER"\n' % (run, "\n".join(fn))
    out = subprocess.run(["bash", "-c", script], capture_output=True, text=True).stdout.split()
    check("disable.sh: the maker's own off app is not in the ledger, ours and the earlier ones are", out == ["a.ours", "old.one"], out)

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SETUP / "ReportLists.java"), str(ROOT / "tests/ReportListsRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.ReportListsRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every ReportLists case ran", "ALL PASS" in out, out[-300:])
else:
    print("SKIP ReportLists on a JVM: no javac here")
sys.exit(1 if failed else 0)
