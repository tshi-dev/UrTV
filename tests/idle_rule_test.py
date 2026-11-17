#!/usr/bin/env python3
"""«تبدأ شاشة التوقف بعد 2147483 ثانية» (user report): the TV's idle delay was 2147483647 (Integer.MAX: the TV turned its screensaver off) and the row
showed the raw number, and «تغيير» opened the maker's settings, which have no screensaver timer. Now the row says «أبداً (التلفزيون أوقف شاشة التوقف)»
and never opens the maker's settings. The rule runs in tests/IdleRuleRun.java; the wiring is read from the source here.   Run: python3 tests/idle_rule_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p): return p.read_text(encoding="utf-8")


act, rule, words, en = read(SAVER / "SettingsActivity.java"), read(SAVER / "IdleRule.java"), read(SAVER / "Words.java"), read(ROOT / "i18n/en/saver.tsv")
check("no Android in the rule", "import android" not in rule)
lab = act.split("private String idleLabel() {")[1].split("\n    }\n")[0]
check("the row says never for IdleRule.NEVER, before any number is made", "IdleRule.NEVER" in lab and lab.index("NEVER") < lab.index("ثانية") and "أبداً (التلفزيون أوقف شاشة التوقف)" in lab)
summ = act.split("private String idleSummary() {")[1].split("\n    }\n")[0]
check("the hub line never says «تبدأ بعد أبداً»", 'if (k == IdleRule.NEVER) return "لا تبدأ: التلفزيون أوقف شاشة التوقف";' in summ and "idleSummary()" in act)
pick = act.split("private void pickIdle() {")[1].split("\n    }\n")[0]
check("«بدء شاشة التوقف بعد» never opens the maker's settings", "openDreamSettings" not in act and "ACTION_DREAM_SETTINGS" not in act and "ACTION_SETTINGS" not in pick)
check("without WRITE_SETTINGS: one line, nothing opened", pick.index("canWriteSettings") < pick.index("toast(") < pick.index("return;"))
for w in ("أبداً (التلفزيون أوقف شاشة التوقف)", "لا تبدأ: التلفزيون أوقف شاشة التوقف", "غيّرها بأمر من مساعدك، إعدادات التلفاز ما فيها مؤقت لها"):
    check("English and the marker for «" + w + "»", ("\n" + w + "\t") in en and 'S.n("' + w + '")' in words)
if not shutil.which("javac"):
    print("SKIP idle rule: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SAVER / "IdleRule.java"), str(ROOT / "tests/IdleRuleRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.IdleRuleRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of IdleRuleRun ran", "ALL PASS" in out, out[-200:])
sys.exit(1 if failed else 0)
