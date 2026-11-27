#!/usr/bin/env python3
"""4.4 beta 4: the summary of «الأذان لكل صلاة» is one rule in two places (the screensaver's settings row, PrayerSummary; the player's row, PrayerScreen.summary).
tests/prayer_summary_cases.tsv drives both, so «كلها صوت وشاشة» / «الفجر: شاشة فقط» / «٢ مختلفة» cannot drift apart. Run: python3 tests/prayer_summary_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

cases = [l.split("\t") for l in (ROOT / "tests/prayer_summary_cases.tsv").read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
if not (shutil.which("javac") and shutil.which("java")):
    print("SKIP prayer summary: no javac here (CI compiles it)")
    sys.exit(0)
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp, "stub"); stub.mkdir()
    Path(stub, "S.java").write_text("package sa.urtv.home; public final class S { public static String t(String s, Object... a) { return a.length == 0 ? s : String.format(s, a); } public static String sep() { return \"، \"; } public static String n(String s) { return s; } }", encoding="utf-8")
    Path(stub, "Context.java").write_text("package android.content; public class Context { }", encoding="utf-8")
    Path(stub, "Prefs.java").write_text("package sa.urtv.home.tilawah; public final class Prefs { public Prefs(android.content.Context c) { } public int hideMask() { return 0; } }", encoding="utf-8")
    T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(stub / "S.java"), str(stub / "Context.java"), str(stub / "Prefs.java"), str(T / "PrayerScreen.java"),
                        str(ROOT / "tests/PrayerSummaryHomeRun.java"), str(ROOT / "android/app/src/main/java/sa/urtv/screensaver/PrayerSummary.java"), str(ROOT / "tests/PrayerSummaryAppRun.java")], capture_output=True, text=True)
    check("both summaries compile on a JVM", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        feed = "\n".join(c[0] for c in cases) + "\n"
        for who, cls in (("the player's row (PrayerScreen.summary)", "sa.urtv.home.tilawah.PrayerSummaryHomeRun"), ("the settings row (PrayerSummary)", "sa.urtv.screensaver.PrayerSummaryAppRun")):
            out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, cls], input=feed, capture_output=True, text=True, encoding="utf-8").stdout.splitlines()
            check(f"{who} gives the table's words for all {len(cases)} cases", out == [c[1] for c in cases], list(zip([c[0] for c in cases], out, [c[1] for c in cases])))
check("a state string that is not five digits says nothing (the row keeps its fixed line)", 'PrayerSummary.of(prayersSeen' in (ROOT / "android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java").read_text(encoding="utf-8")
      and "if (st == null || st.length() != 5) return null;" in (ROOT / "android/app/src/main/java/sa/urtv/screensaver/PrayerSummary.java").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
