#!/usr/bin/env python3
"""GitHub #15 «التاريخ الهجري»: the Gregorian date alone. The whole way: the settings row and `saver.sh hijri` -> the screensaver's preference -> the fragment's
&nohijri=1 -> the page (CONFIG.noHijri, the CSS, the designs' day line) and the home's own date line (DateNum.hijriOff, lineGreg)."""
import shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
rd = lambda p: (ROOT / p).read_text(encoding="utf-8")
A = "android/app/src/main/java/sa/urtv/screensaver/"; H = "android/launcher/src/main/java/sa/urtv/home/"
failed = 0
def check(n, ok, d=""):
    global failed
    print(("PASS " if ok else "FAIL ") + n + ("" if ok else f"  -> {d}")); failed += not ok
prefs, rec, setx, hub = rd(A + "Prefs.java"), rd(A + "SaverReceiver.java"), rd(A + "SettingsActivity.java"), rd(A + "SettingsHub.java")
check("on by default (the Hijri date shows as before); off puts &nohijri=1 in the fragment, and only then",
      'boolean hijriShown() { return sp.getBoolean("hijriShown", true); }' in prefs and '(hijriShown() ? "" : "&nohijri=1")' in prefs)
check("`saver.sh hijri on|off` reaches the receiver (extra hijriline: on or off, nothing else), sets the preference and is in the status",
      "hijri)    set1 hijriline" in rd("optimize/saver.sh") and '{"hijriline", hijriLine}' in rec and 'p.setHijriShown("on".equals(hijriLine))' in rec and '"\\nhijriline: "' in rec)
check("the settings row «التاريخ الهجري» (مفعّل / متوقف) next to «التاريخ بالأرقام», and the hub knows it in «اللغة والموقع» on both screens",
      'row("التاريخ الهجري", prefs.hijriShown() ? "مفعّل" : "متوقف"' in setx and '{"التاريخ الهجري", LANG}' in hub and '{"التاريخ الهجري", "both"}' in hub)
tpl = rd("src/template.html")
check("the page reads #nohijri=1, hides the Hijri line and gives the Gregorian one its place, and the screensaver designs' day line is the Gregorian alone",
      "noHijri: _Q.get('nohijri') === '1'" in tpl and "html.nohijri .hijri{display:none}" in tpl and "html.nohijri .greg{" in tpl and "classList.toggle('nohijri', !!CONFIG.noHijri)" in tpl
      and "const sdDay = () => CONFIG.noHijri ? sdText('greg')" in tpl)
main = rd(H + "MainActivity.java")
check("the home's own date line: Arabic words, numbers and English all drop the Hijri half when asked",
      'if (DateNum.hijriOff(cfg.fragment)) hijri = "";' in main and "noHijri ? DateNum.lineGreg(day.format(now.getTime()), n, true)" in main and "noHijri ? DateNum.lineGreg(day.format(now.getTime()), n, false)" in main and "if (noHijri) return day.format(now.getTime()) + \", \" + g;" in main)
check("it is documented with its command, and the 4.3.2 places are untouched (a new row, no old one moved)", "optimize/saver.sh hijri off" in rd("docs/SETTINGS.md") and "التاريخ الهجري\tHijri date" in rd("i18n/en/saver.tsv"))
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / H / "DateNum.java"), str(ROOT / "tests/DateHijriRun.java")], check=True)
        got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.DateHijriRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
    check("DateNum.hijriOff reads only a whole nohijri=1 part", got["off"] == "true true false false false", got["off"])
    check("the Gregorian-only line: the weekday and the Gregorian numbers, Arabic and English; the full line is unchanged",
          got["line ar"] == "الاثنين، 16/11/2026" and got["line en"] == "Monday, 16/11/2026" and got["with hijri"] == "Monday, 06/06/1448 AH  –  16/11/2026" and got["no parts"] == "Monday", got)
else:
    print("SKIP hijri line on a JVM: no javac"); failed += 1
sys.exit(1 if failed else 0)
