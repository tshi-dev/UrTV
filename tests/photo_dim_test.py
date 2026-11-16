#!/usr/bin/env python3
"""4.4: the photo's own darkness is --pdim (a number); --dim is a COLOUR on :root, and a colour read as an opacity is invalid at computed-value time, which means
opacity 1: a solid black photo layer (the report of a TV that stayed near-black).   Run: python3 tests/photo_dim_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


page = (ROOT / "src/template.html").read_text(encoding="utf-8")
check("--dim stays a colour on :root", re.search(r":root\{[^}]*--dim:#[0-9A-Fa-f]{6}", page, re.S) is not None)
bad = re.findall(r"opacity\s*:[^;}]*var\(--dim", page)
check("no opacity reads var(--dim): a colour there makes the layer opaque", not bad, bad)
check("the photo's darkness is --pdim, with 0 as the value before a photo is shown",
      "opacity:calc(1 - (1 - var(--pdim,0)) * (1 - var(--walldim,0)))" in page and "ph.style.setProperty('--pdim'," in page and "getPropertyValue('--pdim')" in page
      and "setProperty('--dim'" not in page)
sys.exit(1 if failed else 0)
