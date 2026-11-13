#!/usr/bin/env python3
"""4.3.1 the CI tool of «البطولات الكبرى» (.github/tools/tournaments_fixtures.py) with TheSportsDB mocked: three days a tournament, the games of its league only, once each, in the window; a tournament
that answers nothing keeps its previous games that are still inside the window; a 'try' entry is included only if lookupleague says it is that football league.
    python3 tests/tournaments_fixtures_test.py"""
import datetime
import importlib.util
import json
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / ".github/tools"))
spec = importlib.util.spec_from_file_location("tournaments_fixtures", ROOT / ".github/tools/tournaments_fixtures.py")
tf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tf)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


NOW = datetime.datetime(2026, 11, 13, 12, 0, tzinfo=datetime.timezone.utc)


def ev(i, lid, when, home="A", away="B"):
    return {"idEvent": str(i), "idLeague": str(lid), "strHomeTeam": home, "strAwayTeam": away, "strTimestamp": when, "dateEvent": when[:10]}


asked = []
answers = {"eventsday.php?d=2026-11-12&l=4490": [ev(1, 4490, "2026-11-12T19:45:00")], "eventsday.php?d=2026-11-13&l=4490": [ev(2, 4490, "2026-11-13T19:45:00"), ev(2, 4490, "2026-11-13T19:45:00"), ev(3, 9999, "2026-11-13T19:45:00")],
           "eventsday.php?d=2026-11-14&l=4490": [ev(4, 4490, "2026-11-14T19:45:00"), ev(5, 4490, "2026-11-20T19:45:00")]}


def get(path):
    asked.append(path)
    return {"events": answers.get(path)}


events, notes = tf.collect("4490", NOW, get)
check("three days a tournament: yesterday, today, tomorrow", asked == [f"eventsday.php?d=2026-11-1{d}&l=4490" for d in (2, 3, 4)], asked)
check("its own league's games only, once each, inside the window (a week away and another league's left out)", [e["idEvent"] for e in events] == ["1", "2", "4"], [e["idEvent"] for e in events])
check("the notes say what came", notes == {"asked": 3, "games": 3}, notes)
check("a game is kept only inside the window: the last evening to the end of tomorrow", tf.inside(ev(9, 1, "2026-11-12T06:00:00"), NOW) and not tf.inside(ev(9, 1, "2026-11-11T06:00:00"), NOW) and not tf.inside(ev(9, 1, "2026-11-15T20:00:00"), NOW))
with tempfile.TemporaryDirectory() as tmp:
    old = Path(tmp)
    (old / "tournaments.json").write_text(json.dumps({"events": [ev(7, 4490, "2026-11-13T21:00:00"), ev(8, 4480, "2026-11-13T21:00:00"), ev(6, 4490, "2026-11-01T21:00:00")]}), encoding="utf-8")
    tf.PREV = old
    kept = tf.previous("4490", NOW)
    check("a tournament that answered nothing keeps its previous games still inside the window (and only its own)", [e["idEvent"] for e in kept] == ["7"], kept)
    tf.PREV = None
    check("no previous file is nothing, not an error", tf.previous("4490", NOW) == [])
entry = {"id": "4496", "en": "African Cup of Nations"}
ok, found = tf.resolve_try(entry, lambda p: {"leagues": [{"strLeague": "African Cup of Nations", "strSport": "Soccer"}]})
check("a 'try' entry is included when lookupleague says it is that football league", ok and found == "African Cup of Nations")
ok, _ = tf.resolve_try(entry, lambda p: {"leagues": [{"strLeague": "Some Other League", "strSport": "Soccer"}]})
ok2, _ = tf.resolve_try(entry, lambda p: {"leagues": [{"strLeague": "African Cup of Nations", "strSport": "Basketball"}]})
ok3, _ = tf.resolve_try(entry, lambda p: {})
check("...and not when it is another league, another sport, or nothing", not ok and not ok2 and not ok3)
data = json.loads((ROOT / "assets/shared/tournaments.json").read_text(encoding="utf-8"))
check("the data file is what the tool reads: every tournament has its id and Arabic name (they go to the page as names)", all(t["id"] and t["ar"] for t in data["tournaments"]))
sys.exit(1 if failed else 0)
