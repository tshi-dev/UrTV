#!/usr/bin/env python3
"""4.4 review: the tafsir's page-turning timer belongs to its own list. A redraw of another card (prayers, today, smart: every 15 s) calls
fitTafsir on a list with no li.tafsir; that must not stop the timer of the verse card (it froze a paged tafsir on page 1).   python3 tests/tafsir_timer_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


for rel in ("src/template.html", "dist/tv.html"):
    page = (ROOT / rel).read_text(encoding="utf-8")
    m = re.search(r"function fitTafsir\(ul\)\{(.*?)\n\}\n", page, re.S)
    body = m.group(1) if m else ""
    check(f"{rel}: fitTafsir found", bool(body))
    check(f"{rel}: only this list's own timer is cleared (ul.__tafsirT), never one shared by all lists",
          "clearInterval(ul.__tafsirT)" in body and "clearInterval(TAFSIR_PG.t)" not in body, body[:120])
    check(f"{rel}: the timer started is kept on its list", "ul.__tafsirT = setInterval(" in body)
    # WebView 66: no optional catch binding, no ?., no ??, no object spread in the function
    check(f"{rel}: ES2017 only in fitTafsir (no ?. ?? or optional catch)", not re.search(r"\?\.|\?\?|catch\s*\{", body))
raise SystemExit(1 if failed else 0)
