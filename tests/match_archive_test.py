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
import urllib.error
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
calls_ = []
nm2 = ma.Names(None, online=True)
nm2.search = lambda name, lg: calls_.append(name)
for _ in range(5):
    nm2.id("Nowhere United FC", "4328")
check("a name not found is searched once a run, not once a game", len(calls_) == 1, calls_)
nm2.cache = {"Nowhere United": ""}
nm2.asked = set()
nm2.id("Nowhere United FC", "4328")
check("a miss of an earlier run is searched again (once)", len(calls_) == 2, calls_)
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


# 11. history: TheSportsDB mocked (a season of 4 clubs = 12 games, 2 a round); the season list capped, the rounds fill it; a complete past
# season is not asked again; the Wikipedia check compares the scores
def fake_tsdb(capped=2):
    games = {}
    clubs = ["1", "2", "3", "4"]
    n = 0
    for season in ("2024-2025", "2025-2026", "2026-2027"):
        for i, h in enumerate(clubs):
            for j, a in enumerate(clubs):
                if h != a:
                    n += 1
                    rnd = (n - 1) % 6 + 1
                    games.setdefault(season, []).append({"idEvent": f"{season[:4]}{n}", "idLeague": "4668", "strSeason": season,
                        "idHomeTeam": h, "idAwayTeam": a, "strHomeTeam": "Al-" + h, "strAwayTeam": "Al-" + a, "intHomeScore": "1",
                        "intAwayScore": "0", "strStatus": "FT", "dateEvent": f"{season[:4]}-09-{rnd:02d}", "strTimestamp": f"{season[:4]}-09-{rnd:02d}T18:00:00",
                        "intRound": str(rnd), "strSport": "Soccer"})
    asked = []

    def fetch(url, pause=0):
        asked.append(url)
        q = dict(x.split("=", 1) for x in url.split("?", 1)[1].split("&"))
        if "search_all_seasons" in url:
            return {"seasons": [{"strSeason": s} for s in games]}
        evs = games.get(q.get("s"), [])
        if "eventsseason" in url:
            return {"events": evs[:capped]}
        if "eventsround" in url:
            return {"events": [e for e in evs if e["intRound"] == q["r"]] or None}
        return {}
    return fetch, asked


fetch, asked = fake_tsdb()
sys.modules["team_ids"] = SimpleNamespace(API="x/", STATS={"thesportsdb": 0}, PAID=False, redact=str, fetch=fetch, team_key=lambda n: n.lower(),
                                         same_team=lambda a, b: a.lower() == b.lower())
saved = ma.HISTORY
ma.HISTORY = [("4668", "Saudi Pro League", 4)]
tmp = Path(tempfile.mkdtemp())
hargs = lambda: SimpleNamespace(out=str(tmp / "h.json"), report=str(tmp / "r.json"), state=str(tmp / "state.json"), leagues="", seasons=3,
                                budget=1000, pace=0, days=False, check=False)
ma.history(hargs())
rep_ = json.loads((tmp / "r.json").read_text())
check("history: every season read in full through the rounds (12 of 12)", all(v["games"] == 12 and v["finished"] == 12 and v["expected"] == 12
      for v in rep_["4668"]["seasons"].values()), rep_)
check("history: the rounds stop once the season is full", max(int(u.split("r=")[1].split("&")[0]) for u in asked if "eventsround" in u) <= 6, asked)
first = tmp / "first.json"
first.write_text((tmp / "h.json").read_text())
n1 = len(asked)
ma.history(hargs())
again = [u for u in asked[n1:] if "s=2024-2025" in u]
check("history: a complete past season is not asked again; the current one is", again == [] and any("s=2026-2027" in u for u in asked[n1:]))
def fetch404(url, pause=0):
    if "eventsround" in url:
        raise urllib.error.HTTPError(url, 404, "Not Found", {}, None)
    return fetch(url, pause)
sys.modules["team_ids"].fetch = fetch404
ma.history(SimpleNamespace(out=str(tmp / "h404.json"), report=str(tmp / "r404.json"), state=None, leagues="", seasons=3, budget=1000, pace=0,
                           days=False, check=False))
r404 = json.loads((tmp / "r404.json").read_text())
check("history: a 404 from a round is an empty answer, counted, not a failed league", r404["requests"]["errors"].get("eventsround.php", 0) > 0
      and "4668" in r404, r404.get("requests"))
sys.modules["team_ids"].fetch = fetch
check("history: the second run gives the current season only", {e["strSeason"] for e in json.loads((tmp / "h.json").read_text())["events"]} == {"2026-2027"})
oH = build(leagues_dir([]), o9, history=str(first), history_state=str(tmp / "state.json"), history_report=str(tmp / "r.json"))
check("history's games land in the store and its files are kept", len([e for e in store_events(oH).values() if e["lg"] == "4668"]) == 36
      and (oH / "history.json").is_file() and (oH / "history-report.json").is_file())
check("history: results/4668.json, the Saudi league's last results", len(json.loads((oH / "results" / "4668.json").read_text())["results"]) == 30)
table = {("Al-1", "Al-2"): ("1", "0"), ("Al-2", "Al-1"): ("2", "2")}
ma.teams_table = lambda: {}
ma.wiki_results = lambda title: (table, None)
games = json.loads(first.read_text())["events"]
chk = ma.wiki_check([e for e in games if e["strSeason"] == "2024-2025"])
c = chk["4668 2024-2025"]
check("the Wikipedia check: same, different and missing scores counted", c["same"] == 1 and c["differ"] == 1 and c["not_in_wiki"] == 10, c)
check("the Wikipedia check names the page", c["page"] == "2024–25 Saudi Pro League")
ma.HISTORY = saved
check("HISTORY puts Saudi first and expects 306 games a season since 2023-24",
      ma.HISTORY[0][0] == "4668" and ma.expected(ma.HISTORY[0][2], "2024-2025") == (306, True) and ma.expected(ma.HISTORY[0][2], "2022-2023") == (240, True))
check("«auto» leagues: expected from the clubs seen, never a sure count", ma.expected("auto", "2024-2025", 12) == (132, False))
check("openfootball's rounds: a number from «Matchday 7», a knockout's words kept", ma.round_of("League, Matchday 7") == "7"
      and ma.round_of("Quarter-finals") == "Quarter-finals")
check("calendar-year leagues (MLS) read by year", ma.calendar_years(["2024-25", "2025-26"]) == ["2024", "2025", "2026"])
del sys.modules["team_ids"]


# 12. the followed leagues' health; the matrix's plan and merge
stH = ma.Store()
nowH = ma.now_utc()
def put_(i, days, scored_):
    t = ma.iso(nowH + dt.timedelta(days=days))
    stH.put({"id": i, "lg": "4668", "s": "2026-2027", "t": t, "h": "1", "a": "2", "hn": "", "an": "", "hs": "1" if scored_ else None,
             "as": "0" if scored_ else None, "st": "FT" if scored_ else "NS", "r": "", "sp": "Soccer", "pp": False, "src": "tsdb"})
put_("a", -20, True); put_("b", -5, False); put_("c", 5, False)
summ, probs = ma.league_health(stH, nowH, {})
check("a followed league's games played 2-10 days ago without a result are a problem", any("Saudi Pro League (4668)" in p and "have no result" in p for p in probs), probs)
put_("b", -5, True)
summ, probs = ma.league_health(stH, nowH, {"4668": {"2024-2025": {"expected": 306, "expected_sure": True, "finished": 300, "current": False}}})
check("results in the last 10 days: no freshness problem", not any("have no result" in p for p in probs if "4668" in p), probs)
stB = ma.Store()
stB.put({"id": "z", "lg": "4668", "s": "2026-2027", "t": ma.iso(nowH - dt.timedelta(days=16)), "h": "1", "a": "2", "hn": "", "an": "", "hs": "1",
         "as": "0", "st": "FT", "r": "", "sp": "Soccer", "pp": False, "src": "tsdb"})
check("an international break (no game for 16 days) is no problem", not any("4668" in p for p in ma.league_health(stB, nowH, {})[1]))
check("a past season short of its sure count is a problem", any("2024-2025: 300 results of 306" in p for p in probs), probs)
check("health lists every followed league with its newest result", summ["4668"]["results_10d"] == 1 and summ["4668"]["last_result"] and len(summ) == len(ma.HISTORY))
import io, contextlib
buf = io.StringIO()
with contextlib.redirect_stdout(buf):
    ma.plan(SimpleNamespace(leagues=""))
plan_ = json.loads(buf.getvalue())
check("the matrix: every followed league, Saudi first", plan_["league"][0] == "4668" and len(plan_["league"]) == len(ma.HISTORY))
buf = io.StringIO()
with contextlib.redirect_stdout(buf):
    ma.plan(SimpleNamespace(leagues="4339, 4668"))
check("the matrix of a rerun: only the leagues asked for, in HISTORY's order", json.loads(buf.getvalue())["league"] == ["4668", "4339"])
md = Path(tempfile.mkdtemp())
(md / "history-4668").mkdir()
(md / "history-4668" / "games.json").write_text(json.dumps({"events": [{"idEvent": "1"}]}))
(md / "history-4668" / "state.json").write_text(json.dumps({"4668": {"2025-2026": {"complete": True}}, "4339": {"x": {}}}))
(md / "history-4668" / "report.json").write_text(json.dumps({"4668": {"name": "Saudi"}, "requests": {"thesportsdb": 3}}))
prevst = md / "prev.json"
prevst.write_text(json.dumps({"4339": {"2024-2025": {"complete": True}}}))
ma.merge_history(SimpleNamespace(dir=str(md), state=str(prevst), expect="4668,4339", out=str(md / "g.json"), state_out=str(md / "s.json"),
                                 report=str(md / "r.json")))
mst, mrp = json.loads((md / "s.json").read_text()), json.loads((md / "r.json").read_text())
check("merge: a league's job replaces only its own seasons; the others' stay", mst["4668"]["2025-2026"]["complete"] and mst["4339"] == {"2024-2025": {"complete": True}}, mst)
check("merge: a league whose job failed is listed (and rerun by itself)", mrp["failed"] == ["history-4339"], mrp)
src_ti = (ROOT / ".github/tools/team_ids.py").read_text()
check("the key comes only from THESPORTSDB_KEY, the free 123 without it, and is taken out of every error",
      'os.environ.get("THESPORTSDB_KEY")' in src_ti and 'redact(url)' in src_ti and '"123"' in src_ti)


# 13. the owner's issue (archive_health.py): the problems, the two copies compared, one issue opened, updated, closed
spec2 = importlib.util.spec_from_file_location("archive_health", ROOT / ".github/tools/archive_health.py")
ah = importlib.util.module_from_spec(spec2)
spec2.loader.exec_module(ah)
hf = Path(tempfile.mkdtemp()) / "health.json"
hf.write_text(json.dumps({"problems": ["Saudi Pro League (4668): 9 games in the last 10 days, none with a result"]}))
ah.count_events = lambda u, b, f: {"a": 100, "b": 98}[b]
pr = ah.problems([("x/urtv-data", "a", "football/archive"), ("x/UrTV", "b", "")], str(hf))
check("health: the archive's own problems and the two copies differing", len(pr) == 2 and "differ" in pr[1] and "100" in pr[1], pr)
ah.count_events = lambda u, b, f: 100
check("health: the same games in both copies is no problem", ah.problems([("x/a", "a", ""), ("x/b", "b", "")], None) == [])
calls, state_ = [], {"open": []}
def fake_gh(*a):
    calls.append(a)
    if a[:2] == ("issue", "list"):
        return SimpleNamespace(returncode=0, stdout=json.dumps(state_["open"]), stderr="")
    if a[:2] == ("issue", "create"):
        state_["open"] = [{"number": 7, "body": a[a.index("--body") + 1]}]
    return SimpleNamespace(returncode=0, stdout="", stderr="")
ah.gh = fake_gh
ah.issue("problem one")
check("issue: opened once with the label", sum(1 for c in calls if c[:2] == ("issue", "create")) == 1 and any("data-health" in c for c in calls if c[:2] == ("issue", "create")))
calls.clear(); ah.issue("problem one")
check("issue: the same problems again change nothing (no mail)", not any(c[:2] in (("issue", "create"), ("issue", "edit")) for c in calls), calls)
calls.clear(); ah.issue("problem two")
check("issue: other problems update the one issue", any(c[:2] == ("issue", "edit") for c in calls) and not any(c[:2] == ("issue", "create") for c in calls))
calls.clear(); ah.issue("")
check("issue: healthy again closes it", any(c[:2] == ("issue", "close") for c in calls))
wf2 = (ROOT / ".github/workflows/league-fixtures.yml").read_text()
check("data-archive copies urtv-data's archive unchanged (builds only when it cannot be read)", "mirror=true" in wf2 and "steps.src.outputs.mirror != 'true'" in wf2)
check("the workflow's actions are pinned to a commit and run on ubuntu-24.04", not re.search(r"uses: actions/\w+@v\d", wf2) and "ubuntu-latest" not in wf2)

wf3 = (ROOT / ".github/workflows/archive-mirror.yml").read_text()
check("Archive mirror: its own timer, off the quarter hours, not behind the fixtures job", "'7,22,37,52 * * * *'" in wf3 and "needs:" not in wf3)
check("Archive mirror: one writer of data-archive with League fixtures' archive job", "group: data-archive" in wf3 and "group: data-archive" in wf2)
check("Archive mirror: copies urtv-data unchanged, then the count check and the one issue", "tshi-dev/urtv-data data" in wf3
      and "archive_health.py --issue" in wf3 and not re.search(r"uses: actions/\w+@v\d", wf3) and "ubuntu-24.04" in wf3)
doc = (ROOT / "docs/MATCH-ARCHIVE.md").read_text()
check("docs/MATCH-ARCHIVE.md describes every file a client reads", all(x in doc for x in ("index.json", "teams/&lt;teamId&gt;.json",
      "h2h/&lt;lowId&gt;-&lt;highId&gt;.json", "results/&lt;leagueId&gt;.json", "store/&lt;leagueId&gt;/&lt;season&gt;.json")))
check("the doc's format number is the code's", f"now `{ma.V}`" in doc and f"at most {ma.LAST}" in doc and f"at most {ma.RESULTS_MAX}" in doc)

# 4.4 issue #70: TheSportsDB listed MLS games of 2026 under «2026» and again under «2026-2027» (the same ids); the second copy stayed «NS»
# after the first got its result, so the health check counted «games played with no result». One copy per id, the one with a result.
import datetime as _dt   # noqa: E402
ago = lambda d: (_dt.datetime.now(_dt.timezone.utc) - _dt.timedelta(days=d)).strftime("%Y-%m-%dT%H:%M:%S")
mls = [ev("m1", "1", "2", ago(4), "3", "2", "FT", lg="4346", season="2026"), ev("m2", "3", "4", ago(3), "2", "1", "FT", lg="4346", season="2026"),
       ev("m3", "5", "6", ago(5), "1", "0", "FT", lg="4346", season="2026")]
stale = [ev("m1", "1", "2", ago(4), lg="4346", season="2026-2027"), ev("m2", "3", "4", ago(3), lg="4346", season="2026-2027")]
st = ma.Store()   # the store as urtv-data has it now: both copies (a load of two files)
for e in mls + stale:
    c = ma.compact(e)
    st.leagues.setdefault((c["lg"], c["s"]), {})[c["id"]] = c
gone = st.dedupe()
ids = {k: s_ for (lg, s_), b in st.leagues.items() for k in b}
check("#70 dedupe: a game under two seasons is kept once, the copy with its result (the stale «NS» one goes)", gone == 2 and st.total() == 3
      and all(st.leagues[("4346", "2026")][k]["st"] == "FT" for k in ("m1", "m2")) and ("4346", "2026-2027") not in st.leagues, (gone, ids))
st2 = ma.Store()   # the next run: the result first, then the stale copy under the other label: refused, never a step back
for e in mls + stale:
    st2.put(ma.compact(e))
check("#70 put: a stale copy under another season never replaces the one with a result, and is not stored twice",
      st2.total() == 3 and all(e_["st"] == "FT" for e_ in st2.events()), [(k, e_["s"], e_["st"]) for (lg, s_), b in st2.leagues.items() for k, e_ in b.items()])
st3 = ma.Store()   # the other way: the season label moves with a newer report (same or better stage): moved, one copy
st3.put(ma.compact(stale[0])); st3.put(ma.compact(mls[0]))
check("#70 put: the newer copy with a result moves the game to its season (one copy)", st3.total() == 1 and ("4346", "2026") in st3.leagues and not st3.leagues.get(("4346", "2026-2027")))
prev70 = Path(tempfile.mkdtemp())   # a whole build on a store with the duplicates: no «without result», nothing «shrank»
(prev70 / "store/4346").mkdir(parents=True)
for season, evs in (("2026", mls), ("2026-2027", stale)):
    (prev70 / f"store/4346/{season}.json").write_text(json.dumps({"id": "4346", "season": season, "league": "American Major League Soccer",
                                                                    "events": {e["idEvent"]: ma.compact(e) for e in evs}}))
o70 = build(leagues_dir([], lg="4346"), prev70)
h70 = json.loads((o70 / "health.json").read_text())
check("#70 build: the duplicates go (not a «store shrank» failure), and MLS has no game «without result»", h70["followed"]["4346"]["without_result"] == 0
      and not any("4346" in p_ for p_ in h70["problems"]) and len(store_events(o70)) == 3, {k: h70[k] for k in ("problems", "events")})
print("\n%s" % ("ALL PASS" if not failed else f"{failed} FAILED"))
sys.exit(1 if failed else 0)
