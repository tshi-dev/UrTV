#!/usr/bin/env python3
"""4.4 «الرياضة»: the sports cards' files (.github/tools/sports_files.py), offline: sports/<league>.json from the archive's store (the table
computed from the season's finished games, each club's last 5, the last 2 days, the next 7, a cup's knockout rounds), written by every
archive build; f1/next.json from Jolpica-F1's answer, asked at most once a day, a failure keeps the last file; the workflow step.
Run: python3 tests/sports_files_test.py"""
import datetime as dt
import importlib.util
import json
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / ".github/tools"))
spec = importlib.util.spec_from_file_location("match_archive", ROOT / ".github/tools/match_archive.py")
ma = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ma)
import sports_files as sf   # noqa: E402
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
ts = lambda d, h=15: (now + dt.timedelta(days=d)).replace(hour=h, minute=0, second=0).strftime("%Y-%m-%dT%H:%M:%S")
def ev(i, h, a, when, hs=None, as_=None, st="NS", lg="4668", r="1"):
    return {"idEvent": i, "idLeague": lg, "idHomeTeam": h, "idAwayTeam": a, "strHomeTeam": "T" + h, "strAwayTeam": "T" + a, "intHomeScore": hs,
            "intAwayScore": as_, "strStatus": st, "strTimestamp": when, "strSeason": "2026-2027", "strSport": "Soccer", "intRound": r, "strPostponed": "no"}
games = [ev("1", "10", "20", ts(-20), "2", "0", "FT"), ev("2", "30", "10", ts(-13), "1", "1", "FT"), ev("3", "20", "30", ts(-6), "0", "3", "FT"),
         ev("4", "10", "30", ts(-1), "3", "1", "FT"), ev("5", "20", "10", ts(2)), ev("6", "30", "20", ts(9), r="16"), ev("6b", "20", "30", ts(30), r="32"), ev("7", "10", "20", ts(-3), "1", "0", "FT", r="11")]
cupg = [ev("c1", "10", "40", ts(-30), "2", "1", "FT", lg="4719", r="16"), ev("c2", "40", "10", ts(-23), "0", "0", "FT", lg="4719", r="16"),
        ev("c3", "10", "50", ts(5), lg="4719", r="125"), ev("c4", "60", "70", ts(-40), "1", "0", "FT", lg="4719", r="3")]
lg_dir = Path(tempfile.mkdtemp())
(lg_dir / "saudi.json").write_text(json.dumps({"league": "Saudi-Arabian Pro League", "id": "4668", "events": games}))
(lg_dir / "afc.json").write_text(json.dumps({"league": "AFC Champions League Elite", "id": "4719", "events": cupg}))
prev = Path(tempfile.mkdtemp())
(prev / "f1").mkdir()
(prev / "f1/next.json").write_text('{"v":1,"name":"Kept GP","asked":"2000-01-01T00:00:00Z","updated":"2000-01-01T00:00:00Z","sessions":[]}')
out = Path(tempfile.mkdtemp()) / "out"
ma.build(SimpleNamespace(leagues=str(lg_dir), prev=str(prev), out=str(out), live=None, live_note=None, backfill=None, backfill_report=None, names=None))

s = json.loads((out / "sports/4668.json").read_text())
check("every archive build writes sports/<league>.json (v 1, its names and season)", s["v"] == 1 and s["id"] == "4668" and s["season"] == "2026-2027" and s["ar"] == "دوري روشن السعودي", s)
tab = [(r["id"], r["p"], r["pts"], r["gf"] - r["ga"], r["f"]) for r in s["table"]]
check("the table: 3 a win, 1 a draw, by points then goal difference; each club's last 5 newest first", tab == [("10", 4, 10, 5, "wwdw"), ("30", 3, 4, 1, "lwd"), ("20", 3, 0, -6, "lll")], tab)
check("the last 2 days' results only, newest first", [g["id"] for g in s["last"]] == ["4"], s["last"])
check("the next 7 days' games only", [g["id"] for g in s["next"]] == ["5"], s["next"])
check("a league has no cup, even with its matchdays 16 and 32 (TheSportsDB's knockout numbers)", "cup" not in s)
c = json.loads((out / "sports/4719.json").read_text())["cup"]["rounds"]
check("a cup: its knockout games by round, named, in the cup's order (a group game left out)", [(r["r"], r["ar"], [g["id"] for g in r["games"]]) for r in c]
      == [("16", "دور الـ١٦", ["c1", "c2"]), ("125", "ربع النهائي", ["c3"])], c)
cp = json.loads((out / "sports/4719.json").read_text())["cup"]
t16 = cp["rounds"][0]["ties"]
check("a two-legged tie: one tie, its legs in order, the aggregate from each club's side", len(t16) == 1 and t16[0]["a"] == "10" and t16[0]["b"] == "40"
      and [g["id"] for g in t16[0]["legs"]] == ["c1", "c2"] and t16[0]["ag"] == [2, 1], t16)
check("a tie still to be played has no aggregate", "ag" not in cp["rounds"][1]["ties"][0], cp["rounds"][1])
check("the league phase: its group's table and how far it is (matchday 3 of 3)", cp["phase"]["md"] == 3 and cp["phase"]["of"] == 3
      and [r["id"] for r in cp["phase"]["groups"][0]] == ["60", "70"], cp["phase"])

# the real shapes (urtv-data's store, 2024-25 and 2025-26): UCL's play-off is «160» of 16 games one season and a two-legged «32» the next;
# AFC Elite's league phase is two groups (west and east) that never meet; a King Cup's «32» is a real round of 32 (one leg)
def g(i, h, a, d, r, hs="1", as_="0"):
    return {"id": i, "lg": "x", "t": ts(d), "h": h, "a": a, "hn": h, "an": a, "hs": hs, "as": as_, "st": "FT", "r": r}
po = [g(f"p{k}{l}", f"A{k}", f"B{k}", -60 + 7 * l + k * .01, "160") for k in range(8) for l in range(2)]
r16 = [g(f"r{k}{l}", f"A{k}", f"C{k}", -40 + 7 * l + k * .01, "16") for k in range(8) for l in range(2)]
c1 = sf.cup(po + r16)
check("UCL 2024-25: a «160» of 16 games is the play-off, before the round of 16", [(r["r"], r["ar"], len(r["ties"])) for r in c1["rounds"]] == [("160", "الملحق", 8), ("16", "دور الـ١٦", 8)], c1["rounds"])
c2 = sf.cup([dict(x, r="32") for x in po] + r16)
check("UCL 2025-26: a two-legged «32» of 8 ties is the play-off too", c2["rounds"][0]["ar"] == "الملحق", c2["rounds"][0]["ar"])
kc = sf.cup([g(f"k{k}", f"A{k}", f"B{k}", -10 + k * .01, "32") for k in range(16)])
check("a King Cup's «32» of 16 single games stays the round of 32", kc["rounds"][0]["ar"] == "دور الـ٣٢", kc["rounds"][0]["ar"])
west = [g(f"w{k}", f"W{k}", f"W{(k + 1) % 4}", -20 + k, str(k + 1)) for k in range(4)]
east = [g(f"e{k}", f"E{k}", f"E{(k + 1) % 4}", -20 + k, str(k + 1), "0", "2") for k in range(4)]
ph = sf.cup(west + east)["phase"]
check("AFC Elite: the league phase as two groups that never meet, each with its own table", len(ph["groups"]) == 2
      and {frozenset(r["id"] for r in t) for t in ph["groups"]} == {frozenset(f"W{k}" for k in range(4)), frozenset(f"E{k}" for k in range(4))}, ph)
qual = [g(f"q{k}", f"Q{k}", f"W{k}", -80 + k * .01, "1") for k in range(4)]   # UCL's qualifying round «1», in July
ph2 = sf.cup(west + qual)["phase"]
check("a qualifying round with the same number (weeks earlier) is not in the league phase", [len(t) for t in ph2["groups"]] == [4] and ph2["md"] == 4, ph2)
check("the F1 file is carried over by a build that does not ask for it", json.loads((out / "f1/next.json").read_text())["name"] == "Kept GP")
size = (out / "sports/4668.json").stat().st_size
check("the file is small", size < 20000, size)

answer = {"MRData": {"RaceTable": {"Races": [{"season": "2026", "round": "24", "raceName": "Abu Dhabi Grand Prix", "date": "2026-12-06", "time": "13:00:00Z",
          "Circuit": {"circuitName": "Yas Marina Circuit", "Location": {"country": "UAE", "locality": "Abu Dhabi"}},
          "FirstPractice": {"date": "2026-12-04", "time": "09:30:00Z"}, "Qualifying": {"date": "2026-12-05", "time": "14:00:00Z"}}]}}}
f = sf.f1_file(answer, "2026-11-16T00:00:00Z")
check("f1: the race, its Arabic name and its sessions in time order (UTC)", f["name"] == "Abu Dhabi Grand Prix" and f["ar"] == "جائزة أبوظبي الكبرى"
      and [(x["k"], x["t"]) for x in f["sessions"]] == [("fp1", "2026-12-04T09:30:00Z"), ("q", "2026-12-05T14:00:00Z"), ("race", "2026-12-06T13:00:00Z")], f)
lv = sf.f1_file({"MRData": {"RaceTable": {"Races": [dict(answer["MRData"]["RaceTable"]["Races"][0], raceName="Las Vegas Grand Prix",
     Circuit={"circuitName": "Las Vegas Strip Street Circuit", "Location": {"country": "USA", "locality": "Las Vegas"}})]}}}, "x")
check("f1: a race named for its city keeps it (Las Vegas, Jolpica's real answer of 2026-11-17), not its country's name", lv["ar"] == "جائزة لاس فيغاس الكبرى", lv["ar"])
check("f1: no race in the answer: none", sf.f1_file({"MRData": {"RaceTable": {"Races": []}}}, "x") is None)
asked = []
fetch = lambda url: (asked.append(url), json.dumps(answer).encode())[1]
o2 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(out), out=str(o2)), fetch)
n1 = json.loads((o2 / "f1/next.json").read_text())
check("f1: a stale file is asked again and replaced (Jolpica's «current/next»)", asked == [sf.F1_URL] and n1["name"] == "Abu Dhabi Grand Prix" and n1.get("asked"), n1)
o3 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(o2), out=str(o3)), fetch)
check("f1: asked at most once a day (a fresh file is kept, no request)", asked == [sf.F1_URL] and json.loads((o3 / "f1/next.json").read_text())["name"] == "Abu Dhabi Grand Prix")
def boom(url):
    raise OSError("no network")
o4 = Path(tempfile.mkdtemp())
sf.f1(SimpleNamespace(prev=str(out), out=str(o4)), boom)
check("f1: no answer keeps the last file", json.loads((o4 / "f1/next.json").read_text())["name"] == "Kept GP")
# after the season's last race Jolpica's «next» is empty: the next season's first race
over = {"MRData": {"RaceTable": {"season": "2026", "round": "24", "Races": []}}}
first = {"MRData": {"RaceTable": {"season": "2027", "Races": [dict(answer["MRData"]["RaceTable"]["Races"][0], season="2027", round="1", raceName="Australian Grand Prix",
         date="2027-03-14", time="05:00:00Z", Circuit={"circuitName": "Albert Park", "Location": {"country": "Australia"}}, FirstPractice={"date": "2027-03-12", "time": "01:30:00Z"},
         Qualifying={"date": "2027-03-13", "time": "05:00:00Z"})]}}}
urls = []
two = lambda url: (urls.append(url), json.dumps(over if url == sf.F1_URL else first).encode())[1]
nx = sf.f1_next(two, "x")
check("f1: after the season's last race, the next season's first race (Jolpica's 2027/1)", urls == [sf.F1_URL, "https://api.jolpi.ca/ergast/f1/2027/1.json"]
      and nx["name"] == "Australian Grand Prix" and nx["ar"] == "جائزة أستراليا الكبرى" and nx["sessions"][-1]["t"] == "2027-03-14T05:00:00Z", (urls, nx))
check("f1check: ok on a real-shaped answer, refused (exit 1) on none", sf.f1_check(lambda url: json.dumps(answer).encode()) == 0
      and sf.f1_check(lambda url: json.dumps({"MRData": {"RaceTable": {"Races": []}}}).encode()) == 1)
fc = (ROOT / ".github/workflows/f1-check.yml").read_text(encoding="utf-8")
check("CI checks the real Jolpica when the F1 code changes, and writes nothing (no push, read-only token)", "sports_files.py f1check" in fc and "contents: read" in fc
      and "git push" not in fc and "sports_files.py" in fc.split("paths:", 1)[1])
wf = (ROOT / ".github/workflows/league-fixtures.yml").read_text(encoding="utf-8")
check("the workflow asks for F1 after the build, before the push, and a failure never fails the run",
      wf.index("match_archive.py build") < wf.index("sports_files.py f1 --prev prev --out out || echo") < wf.index("Push to data-archive"))
sys.exit(1 if failed else 0)
