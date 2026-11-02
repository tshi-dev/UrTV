#!/usr/bin/env python3
"""The Levant's month names (#months=levant): the page's table and the home screen's MonthNames.java are the same twelve, in order, and
both screens use them only when asked; the default is untouched. Needs javac for the JVM part.   Run: python3 tests/month_names_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
m = re.search(r"const LEVANT_MONTHS = \{(.*?)\};", tpl, re.S)
page = re.findall(r"'([^']+)':'([^']+)'", m.group(1)) if m else []
check("page: twelve months, January to December", len(page) == 12 and page[0][0] == "يناير" and page[-1][0] == "ديسمبر", page)
check("page: only #months=levant turns them on", "months: _Q.get('months') === 'levant' ? 'levant' : 'default'," in tpl
      and "const gm = s => CONFIG.months === 'levant' ?" in tpl)
check("page: every Gregorian month name goes through gregFmt (no Gregorian formatter with a month left outside it)",
      not re.search(r"new Intl\.DateTimeFormat\(GREG,\s*\{[^}]*month:", tpl), re.findall(r"new Intl\.DateTimeFormat\(GREG,\s*\{[^}]*month:[^}]*\}", tpl))
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
check("home: the date line goes through MonthNames with the screensaver's setting",
      "MonthNames.of(gregFormat.format(now.getTime()), MonthNames.levant(cfg.fragment))" in main)
app = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
check("app: months=levant goes in the fragment only when chosen (the default fragment is as before)",
      '("levant".equals(months()) ? "&months=levant" : "")' in app)

if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "MonthNames.java"), str(ROOT / "tests/MonthNamesRun.java")], check=True, capture_output=True)
        run = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.MonthNamesRun"], capture_output=True)
    out = run.stdout.decode("utf-8")
    print("".join(l + "\n" for l in out.splitlines() if not l.startswith("TABLE")), end="")
    java = [tuple(l.split("\t")[1:3]) for l in out.splitlines() if l.startswith("TABLE")]
    check("the home screen's table is the page's, in order", java == page, java)
    if run.returncode != 0 or "FAIL" in out:
        failed += 1
else:
    print("SKIP month names JVM part: no javac here")
sys.exit(1 if failed else 0)
