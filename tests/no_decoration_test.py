#!/usr/bin/env python3
"""The owner's standing rule (19 Nov, «I consider this an AI slop»): no AI-look decoration anywhere in UrTV. No coloured edge or accent bars, no
rounded colour stripes, no decorative badges or chips, no gradient used as decoration, no letter-in-circle placeholder crests, no sparkle marks.
Kept because they work: the readability scrims behind text on photos, a bar whose gradient is the data, the glass clock (docs/ui/PROPOSAL.md).
This pins the places that had decoration in 4.4 so it does not come back.   python3 tests/no_decoration_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


tpl = (ROOT / "src/template.html").read_text(encoding="utf-8")
main = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")


def rule(sel):
    m = re.search(r"(?:^|[\s}])" + re.escape(sel) + r"\{([^}]*)\}", tpl, re.M)
    return m.group(1) if m else ""


check("the cards' heading line is a plain line, no fade", "content:'';flex:1;height:1px;background:rgba(var(--gold-rgb),.32)" in tpl and not re.search(r"\.hd::after\{[^}]*gradient", tpl))
check("no accent line on a card's edge (cs-dark1)", "inset -3px 0 0 var(--gold)" not in tpl)
check("the glass card styles are flat fills: no diagonal sheen, no corner highlight",
      "gradient" not in rule("html.cs-glass1 .week::before") and "gradient" not in rule("html.cs-glass3 .week::before") and "radial-gradient" not in rule("html.cs-glass1 .week::after"))
check("the gold rules under the date are plain lines", "gradient" not in rule(".rule.l") and not re.search(r"\.rule\{[^}]*gradient", tpl))
check("the bar before the adhan is one flat fill across", "gradient" not in rule(".adbar"))
check("the competition tag is plain muted text: no box, no border, no chip fill", all(x not in rule(".fmk") for x in ("border", "padding", "background"))
      and "background" not in rule("#mp .fmk"))
check("the download progress: a flat fill and a plain dot, no glow", "gradient" not in rule(".sd-dl .done") and "box-shadow" not in rule(".sd-dl .now"))
check("the home's focus ring is the ring only, no inner glow", "ring.setColors(" not in main and "ring.setColor(0);" in main)
check("docs/ui/PROPOSAL.md states the rule", "no AI-look decoration" in (ROOT / "docs/ui/PROPOSAL.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
