#!/usr/bin/env python3
"""4.3.1 the match archive offline (.github/tools/match_archive.py): the store append-only and idempotent, a corrected score lands, a finished
score never goes back, a backfilled game gives way to TheSportsDB's copy, the derived files, the guards, the live window (no request when
nothing is live), the backfill's names, and the workflow (the live run alone on its timer). Run: python3 tests/match_archive_test.py"""
import datetime as dt
import importlib.util
import json
import re
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("match_archive", ROOT / ".github/tools/match_archive.py")
ma = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ma)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def ev(i, home, away, when, hs="", as_="", st="NS", lg="4328", season="2026-2027"):
    return {"idEvent": i, "idLeague": lg, "idHomeTeam": home, "idAwayTeam": away, "strHomeTeam": "T" + home, "strAwayTeam": "T" + away,
            "intHomeScore": hs or None, "intAwayScore": as_ or None, "strStatus": st, "strTimestamp": when, "strSeason": season,
            "strSport": "Soccer", "intRound": "1", "strPostponed": "no"}


def leagues_dir(events, lg="4328"):
    d = Path(tempfile.mkdtemp())
    (d / "english-premier-league.json").write_text(json.dumps({"league": "English Premier League", "id": lg, "events": events}))
    (d / "report.json").write_text("{}")
    return d


def build(leagues, prev, out=None, **kw):
    out = out or Path(tempfile.mkdtemp()) / "out"
    a = SimpleNamespace(leagues=str(leagues), prev=str(prev), out=str(out), live=None, live_note=None, backfill=None, backfill_report=None,
                        names=None)
    for k, v in kw.items():
        setattr(a, k, v)
    ma.build(a)
    return out


def store_events(out):
    evs = {}
    for f in (out / "store").glob("*/*.json"):
        evs.update(json.loads(f.read_text())["events"])
    return evs


empty = Path(tempfile.mkdtemp())
past, future = "2026-11-01T15:00:00", "2099-01-01T15:00:00"

# 1. the first run, then the same files again: nothing new, nothing lost
L1 = leagues_dir([ev("1", "10", "20", past, "2", "1", "FT"), ev("2", "20", "10", future)])
o1 = build(L1, empty)
h1 = json.loads((o1 / "health.json").read_text())
check("first run stores both games", h1["events"] == 2 and h1["new"] == 2, h1)
o2 = build(L1, o1)
h2 = json.loads((o2 / "health.json").read_text())
check("the same files again: idempotent", h2["events"] == 2 and h2["new"] == 0 and h2["updated_events"] == 0, h2)
check("the store's files are the same", store_events(o1) == store_events(o2))

# 2. a game the leagues no longer list stays (append-only)
o3 = build(leagues_dir([]), o2)
check("a game gone from data-leagues stays in the store", len(store_events(o3)) == 2)
check("no events read is a problem in health.json", "no events read from data-leagues" in json.loads((o3 / "health.json").read_text())["problems"])

# 3. a corrected score lands; a finished score never goes back to «not started»
o4 = build(leagues_dir([ev("1", "10", "20", past, "3", "1", "FT")]), o3)
check("a corrected score replaces the old one", store_events(o4)["1"]["hs"] == "3", store_events(o4)["1"])
o5 = build(leagues_dir([ev("1", "10", "20", past)]), o4)
check("an older «not started» copy does not undo a result", store_events(o5)["1"]["hs"] == "3" and store_events(o5)["1"]["st"] == "FT")

# 4. a backfilled game (no id) and TheSportsDB's copy of it: one game, TheSportsDB's
bf = Path(tempfile.mkdtemp()) / "bf.json"
bf.write_text(json.dumps({"events": [{"id": "", "lg": "4328", "s": "2025-2026", "t": "2026-03-01T15:00:00Z", "h": "10", "a": "30", "hn": "T10",
                                      "an": "T30", "hs": "1", "as": "1", "st": "FT", "r": "", "sp": "Soccer", "pp": False, "src": "openfootball"}]}))
o6 = build(leagues_dir([]), o5, backfill=str(bf))
check("a backfilled game is kept by date and teams", "d:2026-03-01:10:30" in store_events(o6))
o7 = build(leagues_dir([ev("99", "10", "30", "2026-03-01T15:00:00", "1", "1", "FT", season="2025-2026")]), o6)
e7 = store_events(o7)
check("TheSportsDB's copy replaces the backfilled one", "99" in e7 and "d:2026-03-01:10:30" not in e7, list(e7))
o8 = build(leagues_dir([]), o7, backfill=str(bf))
check("the backfill again does not bring the duplicate back", "d:2026-03-01:10:30" not in store_events(o8))

# 5. the derived files
t10 = json.loads((o8 / "teams" / "10.json").read_text())
check("teams/10.json: the last results, newest first", [x["id"] for x in t10["last"]] == ["1", "99"], t10["last"])
check("teams/10.json: from its side", t10["last"][0]["gf"] == "3" and t10["last"][0]["home"] is True)
check("teams/10.json: the next game", [x["id"] for x in t10["next"]] == ["2"], t10["next"])
h = json.loads((o8 / "h2h" / "10-20.json").read_text())
check("h2h/<low>-<high>.json holds the finished meetings only", [m["id"] for m in h["meetings"]] == ["1"], h)
check("h2h ids ordered by number, not text", (o8 / "h2h" / "10-30.json").is_file())
idx = json.loads((o8 / "index.json").read_text())
check("index.json lists the teams and the league", "10" in idx["teams"] and "4328" in idx["leagues"], idx)

res = json.loads((o8 / "results" / "4328.json").read_text())
check("results/<league>.json: the finished games, newest first", [g["id"] for g in res["results"]] == ["1", "99"] and res["v"] == 1, res)
check("results games carry names and scores", res["results"][0]["hn"] == "T10" and res["results"][0]["hs"] == "3")
o8b = build(leagues_dir([]), o8)
check("a file a run did not change keeps its stamp, byte for byte",
      all((o8 / r).read_bytes() == (o8b / r).read_bytes() for r in ("teams/10.json", "h2h/10-20.json", "results/4328.json", "index.json")))
check("every file says its format (v)", all(json.loads(f.read_text()).get("v") == 1 for f in o8b.rglob("*.json") if f.name != "health.json"))

many = [ev(str(100 + i), "10", "40", f"2026-0{1 + i % 9}-{10 + i:02d}T15:00:00", "1", "0", "FT") for i in range(15)]
o9 = build(leagues_dir(many), o8)
check(f"teams/<id>.json keeps the last {ma.LAST}", len(json.loads((o9 / "teams" / "10.json").read_text())["last"]) == ma.LAST)
check(f"results/<league>.json keeps the last {ma.RESULTS_MAX}", len(json.loads((o9 / "results" / "4328.json").read_text())["results"]) == min(17, ma.RESULTS_MAX))
check("a changed file gets the new stamp", json.loads((o9 / "teams" / "10.json").read_text())["updated"] == json.loads((o9 / "health.json").read_text())["updated"])

# 6. the guards: a broken previous archive is a red run, never a fresh empty store
bad = Path(tempfile.mkdtemp())
(bad / "health.json").write_text(json.dumps({"events": 500}))
try:
    build(L1, bad)
    check("an unreadable archive stops the run", False, "no error")
except SystemExit as e:
    check("an unreadable archive stops the run", "refusing" in str(e), e)
broken = Path(tempfile.mkdtemp())
(broken / "store" / "4328").mkdir(parents=True)
(broken / "store" / "4328" / "x.json").write_text("{not json")
try:
    build(L1, broken)
    check("a broken store file stops the run", False, "no error")
except ValueError:
    check("a broken store file stops the run", True)

# 7. pruning: only the seasons past KEEP_SEASONS go
st = ma.Store()
for y in range(2010, 2022):
    st.put({"id": str(y), "lg": "4328", "s": f"{y}-{y + 1}", "t": f"{y}-09-01T15:00:00Z", "h": "1", "a": "2", "hn": "", "an": "", "hs": "1",
            "as": "0", "st": "FT", "r": "", "sp": "Soccer", "pp": False, "src": "tsdb"})
check("prune drops the oldest seasons only", st.prune(None) == 12 - ma.KEEP_SEASONS and ("4328", "2021-2022") in st.leagues)

# 8. the live window: nothing live, no request; a game in play is asked
calls = []
sys.modules["team_ids"] = SimpleNamespace(API="x/", STATS={"thesportsdb": 0},
                                         fetch=lambda url, pause=0: calls.append(url) or {"events": [ev("2", "20", "10", future, "1", "0", "2H")]})
note = Path(tempfile.mkdtemp()) / "note.json"
live_out = note.parent / "live.json"
ma.live(SimpleNamespace(prev=str(o9), out=str(live_out), note=str(note)))
check("nothing live: no request, no patch", calls == [] and not live_out.exists() and json.loads(note.read_text())["live_games"] == 0)
now = ma.now_utc().replace(microsecond=0)
on = (now - dt.timedelta(minutes=50)).strftime("%Y-%m-%dT%H:%M:%S")
ended = (now - dt.timedelta(hours=2, minutes=50)).strftime("%Y-%m-%dT%H:%M:%S")
long_ago = (now - dt.timedelta(hours=5)).strftime("%Y-%m-%dT%H:%M:%S")
oL = build(leagues_dir([ev("2", "20", "10", on), ev("5", "50", "60", ended), ev("6", "50", "70", long_ago)]), o9)
ma.live(SimpleNamespace(prev=str(oL), out=str(live_out), note=str(note)))
check("in play and ended within 30 minutes are asked, older not", sorted(c.split("=")[-1] for c in calls) == ["2", "5"], calls)
oL2 = build(leagues_dir([]), oL, live=str(live_out), live_note=str(note))
check("the live copy lands in the store", store_events(oL2)["2"]["hs"] == "1" and store_events(oL2)["2"]["st"] == "2H")
check("health.json says the live run", json.loads((oL2 / "health.json").read_text()).get("live_games") == 2)

# 9. the backfill's names
del sys.modules["team_ids"]   # the real one again (no request: online=False)
check("clean_name drops the country and the year", ma.clean_name("Bologna FC 1909 (ITA)") == "Bologna FC" and ma.clean_name("FC Twente '65") == "FC Twente")
check("norm: the same club written two ways", ma.norm("Brighton & Hove Albion FC") == ma.norm("Brighton and Hove Albion"))
nm = ma.Names(None, online=False)
check("a backfill name maps through teams.json", nm.id("FC Bayern München", "4331") == "133664" and nm.id("Olympique de Marseille", "4334") == "133707")
check("a shared common word is not a match (Luton Town is not Ipswich Town)", nm.id("Luton Town FC", "4328") is None)
check("a European cup's names are matched exactly only", nm.id("FC Red Bull Salzburg (AUT)", "4480") is None and nm.id("AC Sparta Praha (CZE)", "4480") is None)
check("the hand file comes first", nm.id("FC Internazionale Milano (ITA)", "4480") == "133681")
names = json.loads((ROOT / ".github/tools/archive_names.json").read_text())
check("archive_names.json maps names to TheSportsDB ids", isinstance(names, dict) and all(re.fullmatch(r"\d{1,7}|", str(v)) for v in names.values()))

# 10. the workflow: the archive after the fixtures, the live run alone on its timer, no key, no request in «archive»
wf = (ROOT / ".github/workflows/league-fixtures.yml").read_text()
check("the live timer runs every 15 minutes", "*/15 * * * *" in wf)
check("the fixtures job does not run on the live timer", "github.event.schedule == '23 */6 * * *'" in wf.split("fixtures:")[1].split("steps:")[0])
check("the archive job follows the fixtures, red or green", "needs: fixtures" in wf and "always() && needs.fixtures.result != 'cancelled'" in wf)
check("the archive pushes only data-archive", re.findall(r"push -q?f\S* .*?(data-\w+)", wf.split("archive:")[-1]) == ["data-archive"], wf[-400:])
src = (ROOT / ".github/tools/match_archive.py").read_text()
check("build asks no network", "urlopen" not in src.split("def build(")[1].split("\ndef ")[0] and "fetch(" not in src.split("def build(")[1].split("\ndef ")[0])
check(f"the live run asks at most {ma.LIVE_MAX}, {ma.LIVE_GAP} s apart (the free key's 30 a minute)", ma.LIVE_MAX <= 40 and 60 / ma.LIVE_GAP <= 30)
check("no Israel, no Western Sahara in the archive's names", not re.search(r"israel|western sahara|إسرائيل|الصحراء الغربية", src + wf, re.I))

doc = (ROOT / "docs/MATCH-ARCHIVE.md").read_text()
check("docs/MATCH-ARCHIVE.md describes every file a client reads", all(x in doc for x in ("index.json", "teams/&lt;teamId&gt;.json",
      "h2h/&lt;lowId&gt;-&lt;highId&gt;.json", "results/&lt;leagueId&gt;.json", "store/&lt;leagueId&gt;/&lt;season&gt;.json")))
check("the doc's format number is the code's", f"now `{ma.V}`" in doc and f"at most {ma.LAST}" in doc and f"at most {ma.RESULTS_MAX}" in doc)

print("\n%s" % ("ALL PASS" if not failed else f"{failed} FAILED"))
sys.exit(1 if failed else 0)
