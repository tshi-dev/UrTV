#!/usr/bin/env python3
"""4.3.1 «البطولات الكبرى»: the games of today and tomorrow (and the last evening's results) of the big tournaments in assets/shared/tournaments.json,
as ONE small file, leagues/tournaments.json, that the page downloads once every 30 minutes when the person's switch is on (no TheSportsDB request from the TV).
Run in CI (.github/workflows/league-fixtures.yml, its own job and its own timer): TheSportsDB is not reachable from the dev container, so the report says what each call
answered in the real run.

  {"updated": "...Z", "names": {"4490": "<Arabic name>"}, "events": [...the league-fixtures events...], "wanted_not_found": [...], "tried": {...}}

Each tournament is asked for three days (yesterday, today, tomorrow: eventsday.php?d=DAY&l=ID). A tournament whose calls all came back empty keeps its previous
games that are still in the window (TOURNAMENTS_PREV is a directory holding the last file). The "try" entries of the data file are looked up by id first
(lookupleague.php) and included only if the answer is a football league named like the entry; the report says what they were. The page shows nothing from a file older than 36 h.

    python3 .github/tools/tournaments_fixtures.py
"""
import datetime
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import league_fixtures as lf  # noqa: E402
import team_ids as ti  # noqa: E402

ROOT = HERE.parents[1]
DATA = ROOT / "assets/shared/tournaments.json"
OUT = Path(os.environ.get("LEAGUES_OUT", "leagues"))
PREV = Path(os.environ["TOURNAMENTS_PREV"]) if os.environ.get("TOURNAMENTS_PREV") else None
BACK_H, AHEAD_H = 30, 54   # the window of the games kept: the last evening to the end of tomorrow, in any time zone


def window(now):
    return now - datetime.timedelta(hours=BACK_H), now + datetime.timedelta(hours=AHEAD_H)


def days(now):
    first = (now - datetime.timedelta(days=1)).date()
    return [(first + datetime.timedelta(days=i)).isoformat() for i in range(3)]


def inside(e, now):
    k = lf.kickoff(e)
    lo, hi = window(now)
    return k is not None and lo <= k <= hi


def collect(lid, now, get=ti.get):
    """-> (events, notes) of one tournament: its games of the three days, once each, in the window."""
    raw = []
    for day in days(now):
        raw += get(f"eventsday.php?d={day}&l={lid}").get("events") or []
    seen, out = set(), []
    for e in raw:
        if (not e.get("idEvent") or e["idEvent"] in seen or not (e.get("strHomeTeam") and e.get("strAwayTeam"))
                or str(e.get("idLeague") or lid) != str(lid) or not inside(e, now)):
            continue
        seen.add(e["idEvent"])
        out.append(e)
    out.sort(key=lambda e: (lf.kickoff(e), e["idEvent"]))
    return [lf.trim(e) for e in out], {"asked": len(days(now)), "games": len(out)}


def resolve_try(entry, get=ti.get):
    """A 'try' entry: -> True when lookupleague says this id is a football league of that name."""
    got = (get(f"lookupleague.php?id={entry['id']}").get("leagues") or [{}])[0]
    name = (got.get("strLeague") or "").lower()
    words = [w for w in entry["en"].lower().replace("african", "africa").split() if len(w) > 3]
    return got.get("strSport") == "Soccer" and bool(words) and all(w in name.replace("african", "africa") for w in words), got.get("strLeague") or ""


def previous(lid, now):
    if not PREV:
        return []
    try:
        old = json.loads((PREV / "tournaments.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return []
    return [e for e in old.get("events") or [] if str(e.get("idLeague")) == str(lid) and inside(e, now)]


def main():
    data = json.loads(DATA.read_text(encoding="utf-8"))
    now = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    tournaments = list(data["tournaments"])
    report, problems, tried = {"leagues": {}}, [], {}
    for entry in data.get("try") or []:
        try:
            ok, found = resolve_try(entry)
        except Exception as e:  # noqa: BLE001
            ok, found = False, f"error: {str(e)[:80]}"
        tried[entry["id"]] = {"asked": entry["en"], "answered": found, "included": ok}
        if ok:
            tournaments.append(entry)
    events, names = [], {}
    for t in tournaments:
        names[t["id"]] = t["ar"]
        try:
            got, notes = collect(t["id"], now)
        except Exception as e:  # noqa: BLE001
            got, notes = [], {"error": str(e)[:200]}
            problems.append(f"{t['en']}: {e}")
        if not got:
            got = previous(t["id"], now)
            notes["kept"] = len(got)
        report["leagues"][t["en"]] = notes
        events += got
        print(f"{t['en']}: {len(got)} games {json.dumps(notes, ensure_ascii=False)}", flush=True)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "tournaments.json").write_text(json.dumps(
        {"updated": now.isoformat().replace("+00:00", "Z"), "names": names, "events": events,
         "wanted_not_found": data.get("wanted_not_found") or [], "tried": tried}, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    report["requests"] = ti.STATS["thesportsdb"]
    report["problems"] = problems
    (OUT / "tournaments-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"{len(events)} games of {len(tournaments)} tournaments; tried: {json.dumps(tried, ensure_ascii=False)}")
    if problems:
        print("\nPROBLEMS:\n- " + "\n- ".join(problems))
        sys.exit(1)


if __name__ == "__main__":
    main()
