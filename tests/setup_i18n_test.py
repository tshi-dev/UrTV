#!/usr/bin/env python3
"""«إعداد تلفزيوني» in English mode (tester GO 178): every fixed Arabic sentence SetupRunner puts on the start, plan, wait and progress
screens is listed in SetupActivity.WORDS (S.n, so i18n/en/setup.tsv must translate it), the Activity shows them through S.tv / the TR hook,
and its own labels go through S.t. Text is laid out with Dir (start gravity and text direction), not a fixed right.   python3 tests/setup_i18n_test.py"""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
D = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
run = (D / "SetupRunner.java").read_text(encoding="utf-8")
act = (D / "SetupActivity.java").read_text(encoding="utf-8")
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


AR = re.compile("[؀-ۿ]")
lit = lambda seg: [m for m in re.findall(r'"((?:[^"\\]|\\.)*)"', seg) if AR.search(m)]
words = set(re.findall(r'S\.n\("((?:[^"\\]|\\.)*)"\)', act[act.index("static final String[] WORDS"):act.index("static final String[] ENDS")]))
need = lit(run[run.index("static String label(String id)"):run.index("/** The start screen")])
need += lit(run[run.index("static List<String> planLines(Preview p, java"):run.index("/** The precheck, then")])
for n in ("PROMPT_LINE", "PROMPT_LATE", "NOT_STARTED"):
    need += lit(re.search(n + r" = (.*?);\n", run).group(1))
need.append("".join(re.findall(r'"((?:[^"\\]|\\.)*)"', re.search(r"WIFI_ASK_TEXT = (.*?);\n", run, re.S).group(1))))
missing = [w for w in need if w not in words]
check("every fixed sentence of SetupRunner's labels, plan, extras line and prompt/Wi-Fi texts is in SetupActivity.WORDS", not missing, missing[:3])
check("the screen shows them through the table", all(x in act for x in ("S.tv(SetupRunner.label(now.id))", "S.tv(SetupRunner.PROMPT_LATE)", "S.tv(SetupRunner.PROMPT_LINE)",
      "S.tv(SetupRunner.WIFI_ASK_TEXT)", "SetupRunner.planLines(p, TR)", "TR.apply(a.why)", "say(S.tv(e.text)")))
body = act[act.index("private void buildStart"):act.index("private void buildDone")]
bare = [l.strip() for l in body.splitlines() if re.search(r'(text|button|say)\([^S\n]*"[^"]*[؀-ۿ]', l) and "S.t" not in l]
check("no Arabic literal of the start, plan, progress or wait screens is shown without S.t", not bare, bare[:3])
check("the layout follows the language (Dir): text start gravity and direction", "t.setGravity(Dir.start())" in act and "t.setTextDirection(Dir.text())" in act)
sys.exit(1 if failed else 0)
