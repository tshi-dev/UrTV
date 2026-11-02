#!/usr/bin/env python3
"""assets/shared/broadcast.json, the rights holders' marks of «مباريات اليوم» (docs/PROPOSALS-NOV2.md §1): every row has its source link
and its seasons, and no row outlives its deal. A row whose last season has ended fails (the screen already hides it: it must be checked
again or removed); one ending within WARN_DAYS is named as a warning, so it is checked before the new season starts.

    python3 .github/tools/broadcast_check.py           exit 1 on a bad or lapsed row
"""
import datetime
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FILE = ROOT / "assets/shared/broadcast.json"
WARN_DAYS = 31
SEASON = re.compile(r"^(\d{4})(?:-(\d{2}))?$")
NEED = ("idLeague", "competition", "ar", "en", "from", "until", "source", "confidence", "whole", "verified")


def season_end(s):
    """The last day of a season: «2027-28» ends 31 July 2028 (as the page's seasonEnd), «2026» on 31 December 2026; None when unreadable."""
    m = SEASON.match(s or "")
    if not m:
        return None
    y = int(m.group(1))
    if m.group(2):
        if int(m.group(2)) != (y + 1) % 100:
            return None
        return datetime.date(y + 1, 7, 31)
    return datetime.date(y, 12, 31)


def problems(rows, today):
    """-> (errors, warnings) as lines."""
    errors, warnings, seen = [], [], set()
    for n, r in enumerate(rows, 1):
        name = f"row {n} ({r.get('competition') or r.get('idLeague') or '?'})"
        missing = [k for k in NEED if r.get(k) in (None, "")]
        if missing:
            errors.append(f"{name}: no {', '.join(missing)}")
            continue
        if not re.fullmatch(r"\d{1,9}", str(r["idLeague"])):
            errors.append(f"{name}: idLeague is TheSportsDB's number")
        if str(r["idLeague"]) in seen:
            errors.append(f"{name}: a second row for league {r['idLeague']}")
        seen.add(str(r["idLeague"]))
        if not re.fullmatch(r"https://\S+", r["source"]):
            errors.append(f"{name}: the source is an https link")
        if r["confidence"] not in ("high", "medium", "low"):
            errors.append(f"{name}: confidence is high, medium or low")
        if not isinstance(r["whole"], bool):
            errors.append(f"{name}: whole is true or false")
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(r["verified"])):
            errors.append(f"{name}: verified is the date (YYYY-MM-DD) the source was read")
        start, end = season_end(r["from"]), season_end(r["until"])
        if start is None or end is None or end < start:
            errors.append(f"{name}: from and until are seasons, «2025-26» or «2026», in order")
            continue
        if end < today:
            errors.append(f"{name}: its last season {r['until']} ended on {end}: check the rights again or remove the row")
        elif (end - today).days <= WARN_DAYS:
            warnings.append(f"{name}: its last season {r['until']} ends on {end}: check the rights before the new season")
    return errors, warnings


def main(path=FILE, today=None):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    rows = data.get("rows")
    if not isinstance(rows, list):
        print("FAIL broadcast.json: no rows list")
        return 1
    errors, warnings = problems(rows, today or datetime.date.today())
    for w in warnings:
        print("WARN broadcast.json " + w)
    for e in errors:
        print("FAIL broadcast.json " + e)
    if not errors:
        print(f"PASS broadcast.json: {len(rows)} rows, each with its source and seasons, none lapsed")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
