#!/usr/bin/env python3
"""User report «ماهو راضي يفعل تصحيح USB»: Developer options open from the build number, whose place differs per brand. The setup's box (under
«قبل البدء») and the rescue app's box list the path for TCL, Xiaomi, Sony and Skyworth, one line each, the same lines in both, each with its English.
Run: python3 tests/dev_help_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return (ROOT / p).read_text(encoding="utf-8")
act = read("android/launcher/src/main/java/sa/urtv/home/setup/SetupActivity.java")
res = read("android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java")
def lines(src): return re.findall(r'(?:S\.n\()?"([^"]*)"\)?,\n', src.split("static final String[] DEV_HELP = {")[1].split("};")[0])
setup, rescue = lines(act), lines(res)
check("the setup has the lines", len(setup) == 6, setup)
for brand in ("TCL:", "شاومي:", "سوني:", "سكاي ورث:"):
    check("a line for " + brand, any(l.startswith(brand) and "حول" in l for l in setup))
check("each brand line names both places (System and Device Preferences)", all("النظام ← حول" in l and "تفضيلات الجهاز ← حول ← Build" in l for l in setup[1:5]))
check("the rescue app's box has the very same lines", setup == rescue)
check("no pills or dots as separators", not any("·" in l or "•" in l for l in setup))
start = act.split("private void buildStart(SetupRunner r) {")[1].split("\n    }\n")[0]
check("the setup box shows it right after «قبل البدء», before the buttons", start.index("قبل البدء") < start.index("add(text(devHelp(), 14") < start.index('button(S.t("جهّز كل شي"))'))
check("each line is marked (S.n) and shown through S.tv", "append(S.tv(line))" in act and all('S.n("' + l + '")' in act for l in setup))
check("the rescue box shows it after its debugging line", res.index("يحتاج «تصحيح USB» مفعّلاً") < res.index('String.join("\\n", DEV_HELP)'))
en = read("i18n/en/setup.tsv")
check("each line has its English", all(any(x.startswith(l + "\t") for x in en.splitlines()) for l in setup))
sys.exit(1 if failed else 0)
