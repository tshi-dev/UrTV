#!/usr/bin/env python3
"""A league's fixtures for the coming week and its results of the last day, one small JSON per league, for the
screen's «الدوريات كاملة» (#leagues=). Run in CI (.github/workflows/league-fixtures.yml): TheSportsDB is not
reachable from the dev container, so the limits below were not measured there; the report says what each call
answered in the real run.

Why a file and not the page asking: a league has up to 10 games a round, and asking per club costs 2 requests a
club every 30 minutes. The free API's league calls are capped (the free key shows 15 games from
eventsnextleague.php / eventspastleague.php and 100 of a season from eventsseason.php; team_ids.py works around
the same caps), so a round can be cut short. Here every day of the window is asked on its own
(eventsday.php?d=DAY&l=ID), the two league calls are added, and the union is kept.

Out: leagues/<slug>.json = {"league", "id", "updated", "events": [...]} and leagues/report.json. The page reads
<slug>.json for each followed league (slug() is the same as team_ids.slug(); a branch data-leagues holds them).
An event keeps only what the page uses. A league whose calls all came back empty keeps its previous file (LEAGUES_PREV
is a directory holding it) rather than blanking the screen: "stale": true.

    python3 .github/tools/league_fixtures.py            all leagues of Prefs.LEAGUES except the national teams
    python3 .github/tools/league_fixtures.py NBA        one
"""
import datetime
import json
import os
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import team_ids as ti  # noqa: E402

ROOT = HERE.parents[1]
OUT = Path(os.environ.get("LEAGUES_OUT", "leagues"))
PREV = Path(os.environ["LEAGUES_PREV"]) if os.environ.get("LEAGUES_PREV") else None
BACK_H, AHEAD_DAYS = 36, 7   # results of the last day and a half, fixtures of the coming week
KEEP = ("idEvent", "idLeague", "idHomeTeam", "idAwayTeam", "strHomeTeam", "strAwayTeam", "intHomeScore",
        "intAwayScore", "strStatus", "strTimestamp", "dateEvent", "strTime", "strSport", "strSeason", "intRound",
        "strProgress", "strHomeTeamBadge", "strAwayTeamBadge", "strVenue")   # the badges: the launcher's matches card shows the clubs'
#                                         crests; the venue: «مباريات اليوم» says where a game is played (docs/PROPOSALS-NOV2.md §1)
CAP_GUESS = 15   # what the free league calls seem to hold; the report says if a call returned exactly this


def kickoff(e):
    """Kick-off as a UTC datetime, None when the event has no usable date."""
    s = e.get("strTimestamp") or (f"{e['dateEvent']}T{e.get('strTime') or '00:00:00'}" if e.get("dateEvent") else "")
    if not s:
        return None
    try:
        d = datetime.datetime.fromisoformat(s.replace("Z", "+00:00"))
    except ValueError:
        return None
    return d if d.tzinfo else d.replace(tzinfo=datetime.timezone.utc)


def trim(e):
    return {k: e[k] for k in KEEP if e.get(k) not in (None, "")}


def current_season(events):
    """The newest season named among the events: a window across the turn of a season holds two."""
    seasons = {e.get("strSeason") for e in events if e.get("strSeason")}
    return max(seasons) if seasons else None


def pick(events, lid, now):
    """The events of league lid inside the window, once each, soonest first, of its current season only."""
    lo, hi = now - datetime.timedelta(hours=BACK_H), now + datetime.timedelta(days=AHEAD_DAYS)
    seen, out = set(), []
    for e in events:
        k = kickoff(e)
        if (not e.get("idEvent") or e["idEvent"] in seen or not (e.get("strHomeTeam") and e.get("strAwayTeam"))
                or str(e.get("idLeague") or lid) != str(lid) or k is None or not lo <= k <= hi):
            continue
        seen.add(e["idEvent"])
        out.append(e)
    season = current_season(out)
    out = [e for e in out if not season or not e.get("strSeason") or e["strSeason"] == season]
    out.sort(key=lambda e: (kickoff(e), e["idEvent"]))
    return [trim(e) for e in out]


def days(now):
    first = (now - datetime.timedelta(hours=BACK_H)).date()
    return [(first + datetime.timedelta(days=i)).isoformat() for i in range((now.date() - first).days + AHEAD_DAYS + 1)]


def collect(name, lid, now, get=ti.get):
    """-> (events, notes): what the calls answered, for the report."""
    notes = {"id": lid}
    raw = []
    for key, path in (("next", f"eventsnextleague.php?id={lid}"), ("past", f"eventspastleague.php?id={lid}")):
        got = get(path).get("events") or []
        notes[key] = {"count": len(got), "capped": len(got) == CAP_GUESS,
                      "range": [min((e.get("dateEvent") or "" for e in got), default=""),
                                max((e.get("dateEvent") or "" for e in got), default="")]}
        raw += got
    by, answered = "id", {"id": 0, "name": 0}

    def ask(day):
        got = get(f"eventsday.php?d={day}&l={lid if by == 'id' else name.replace(' ', '_')}").get("events") or []
        answered[by] += len(got)
        return got

    window = days(now)
    for n, day in enumerate(window):
        raw += ask(day)
        # nothing by id on the first days, though the league calls name games in the window: ask by name, those days again too
        if n == 2 and by == "id" and not answered["id"] and pick(raw, lid, now):
            by = "name"
            for early in window[:3]:
                raw += ask(early)
    notes["eventsday"] = {"games by id": answered["id"], "games by name": answered["name"], "last form": by}
    return pick(raw, lid, now), notes


def previous(slug_, now, lid):
    if not PREV:
        return []
    try:
        old = json.loads((PREV / f"{slug_}.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return []
    return pick(old.get("events") or [], lid, now)


def write_report(report, problems):
    report["requests"] = ti.STATS["thesportsdb"]
    report["problems"] = problems
    (OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    names = [n for n in ti.prefs_leagues() if not argv or n in argv]
    cache = ti.load_cache()[0]["leagues"]
    ids = ti.league_ids(names, cache)
    now = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    OUT.mkdir(parents=True, exist_ok=True)
    report, problems = {"requests": 0, "leagues": {}}, []
    for name in names:
        lid = ids.get(name)
        if not lid:
            problems.append(f"{name}: no league id")
            continue
        try:
            events, notes = collect(name, lid, now)
        except Exception as e:   # a league that fails must not stop the others
            events, notes = [], {"error": str(e)[:200]}
            problems.append(f"{name}: {e}")
        stale = not events
        if stale:
            events = previous(ti.slug(name), now, lid)
        notes["events"] = len(events)
        report["leagues"][name] = notes
        # nothing now and nothing kept: still write the (empty) file, so the page can tell «no games» from «no file»
        (OUT / f"{ti.slug(name)}.json").write_text(json.dumps(
            {"league": name, "id": str(lid), "updated": now.isoformat().replace("+00:00", "Z"), "events": events,
             **({"stale": True} if stale and events else {})}, ensure_ascii=False, separators=(",", ":")) + "\n",
            encoding="utf-8")
        print(f"{name}: {len(events)} games  {json.dumps(notes, ensure_ascii=False)}", flush=True)
        write_report(report, problems)   # after every league: a run cut off by its time limit still pushes what it has
    write_report(report, problems)
    if problems:
        print("\nPROBLEMS:\n- " + "\n- ".join(problems))
        sys.exit(1)


if __name__ == "__main__":
    main()
