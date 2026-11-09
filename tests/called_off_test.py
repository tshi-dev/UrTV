#!/usr/bin/env python3
"""4.3 A1: a game TheSportsDB marks postponed or abandoned (its short codes PST and ABD, and AWD for awarded) is never shown as live: the page's
CALLED_OFF knows them, every list that shows games uses it (a followed team, a league, «المباريات», the events), and the built pages carry it.
    python3 tests/called_off_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

for f in ("src/template.html", "dist/tv.html", "dist/tv-en.html"):
    t = (ROOT / f).read_text(encoding="utf-8")
    m = re.search(r"const CALLED_OFF = /(.*?)/i;", t)
    check(f"{f}: CALLED_OFF is there", m is not None)
    if not m:
        continue
    rx = re.compile(m.group(1), re.I)
    off = ["PST", "ABD", "AWD", "Postponed", "POSTP", "CANC", "Cancelled", "Abandoned", "ABAN", "SUSP", "Suspended", "WO"]
    on = ["NS", "1H", "2H", "HT", "ET", "P", "LIVE", "FT", "AET", "PEN", "Match Finished", "", "Not Started", "BT"]
    check(f"{f}: postponed, abandoned, awarded, cancelled, suspended and walk-over are called off", all(rx.match(s) for s in off), [s for s in off if not rx.match(s)])
    check(f"{f}: a game not started, under way, at half time or finished is not", not any(rx.match(s) for s in on), [s for s in on if rx.match(s)])
    over = t[t.index("function eventsOf("):t.index("function eventWhen(")]
    check(f"{f}: the events' list leaves called-off ones out too", "CALLED_OFF.test(e.strStatus" in over, over[:200])
    check(f"{f}: every list of games asks CALLED_OFF (team result, team week, team today, league, the matches page, the cache)", t.count("CALLED_OFF.test(") >= 7, t.count("CALLED_OFF.test("))
sys.exit(1 if failed else 0)
