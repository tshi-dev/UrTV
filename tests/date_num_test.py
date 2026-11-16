#!/usr/bin/env python3
"""4.3.3: the home's Gregorian date has its year, and «التاريخ بالأرقام» (#datenum=1, `saver.sh datenum on`, the settings' «شكل التاريخ») writes
the Hijri and the Gregorian as day/month/year on the home and the screensaver, in the digits «الأرقام» chose. DateNum runs on a plain JVM; the
wiring is read from the source (the page in a browser: tests/date_num.test.mjs).   Run: python3 tests/date_num_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
src = lambda p: p.read_text(encoding="utf-8")
main, prefs, recv, st, saver, tpl = src(HOME / "MainActivity.java"), src(APP / "Prefs.java"), src(APP / "SaverReceiver.java"), src(APP / "SettingsActivity.java"), src(ROOT / "optimize/saver.sh"), src(ROOT / "src/template.html")

check("the home's Gregorian date has the year, in Arabic and English", 'new SimpleDateFormat("d MMMM y", new Locale("ar"))' in main and 'new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)' in main)
check("the home reads #datenum=1 from the settings and writes the numbers before arabicDigits (the digits stay the person's)",
      "if (DateNum.on(cfg.fragment)) {" in main and "date.setText(arabicDigits(hijri.isEmpty() ? greg : hijri" in main
      and main.index("if (DateNum.on(cfg.fragment)) {") < main.index("date.setText(arabicDigits(hijri.isEmpty() ? greg : hijri"))
check("the Hijri numbers move with «فرق التاريخ الهجري», its month counted from 1",
      "h.setTime(shift == 0 ? now.getTime() : HijriShift.date(now, shift));" in main and "h.get(android.icu.util.Calendar.MONTH) + 1" in main)
check("the setting: off by default, #datenum=1 only while on, saver.sh datenum on|off, saver.sh show, the settings row under «الأرقام»",
      'sp.getBoolean("dateNum", false)' in prefs and '(dateNum() ? "&datenum=1" : "")' in prefs and 'p.setDateNum("on".equals(dateNum));' in recv
      and '"\\ndatenum: "' in recv and 'set1 datenum' in saver and st.index('row("الأرقام"') < st.index('row("شكل التاريخ", prefs.dateNum() ? "بالأرقام" : "بالحروف"'))
check("the page: #datenum=1 writes both lines as day/month/year through ar() (the digits chosen)",
      "dateNum: _Q.get('datenum') === '1'," in tpl and "if(CONFIG.dateNum) try{" in tpl and "return ar(p.day + '/' + p.month + '/' + parseInt(p.year));" in tpl)
check("tests/run.sh runs this test and the page's", all(t in src(ROOT / "tests/run.sh") for t in ("tests/date_num_test.py", "tests/date_num.test.mjs")))

if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    (Path(tmp) / "DateNumRun.java").write_text('package sa.urtv.home; public final class DateNumRun { public static void main(String[] a) { System.out.println(String.join("\\n", '
        'String.valueOf(DateNum.on("city=x&datenum=1")), String.valueOf(DateNum.on("datenum=1&x=2")), String.valueOf(DateNum.on("datenum=10")), String.valueOf(DateNum.on(null)), '
        'DateNum.ddmmyyyy(6, 6, 1448), DateNum.ddmmyyyy(16, 11, 2026), DateNum.line("الاثنين", "06/06/1448|16/11/2026", true), DateNum.line("Monday", "06/06/1448|16/11/2026", false))); } }', encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "DateNum.java"), str(Path(tmp) / "DateNumRun.java")], check=True, capture_output=True)
    got = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.DateNumRun"], check=True, capture_output=True).stdout.decode("utf-8").split("\n")
    check("DateNum.on: only datenum=1 itself, anywhere in the fragment", got[:4] == ["true", "true", "false", "false"], got[:4])
    check("DateNum: two-digit day and month, the year whole", got[4:6] == ["06/06/1448", "16/11/2026"], got[4:6])
    check("DateNum.line: «الاثنين، 06/06/1448 هـ  –  16/11/2026», and in English «Monday, … AH  –  …»",
          got[6:8] == ["الاثنين، 06/06/1448 هـ  –  16/11/2026", "Monday, 06/06/1448 AH  –  16/11/2026"], got[6:8])
else:
    print("SKIP the Java cases: no javac")
print(f"{failed} failed" if failed else "date num: all passed")
sys.exit(1 if failed else 0)
